package org.aksw.facete.v3.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.aksw.commons.collections.trees.TreeUtils;
import org.aksw.facete.v3.api.ConstraintFacade;
import org.aksw.facete.v3.api.DataQuery;
import org.aksw.facete.v3.api.FacetDirNode;
import org.aksw.facete.v3.api.FacetNode;
import org.aksw.facete.v3.bgp.api.BgpMultiNode;
import org.aksw.facete.v3.bgp.api.BgpNode;
import org.aksw.jena_sparql_api.concepts.BinaryRelation;
import org.aksw.jena_sparql_api.concepts.UnaryRelation;
import org.aksw.jena_sparql_api.utils.model.ResourceUtils;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.sparql.core.Var;
import org.hobbit.benchmark.faceted_browsing.v2.domain.Vocab;
import org.hobbit.benchmark.faceted_browsing.v2.main.FacetedQueryGenerator;


public class FacetNodeImpl
	implements FacetNodeResource
{
	protected FacetedQueryResource query;
//	protected FacetNodeResource parent;
	//protected Resource state;
	protected BgpNode state;

//	public FacetNodeImpl(FacetedQueryResource query, Resource state) {
//		this(query, state);
//	}

//	public FacetNodeImpl(Resource state) {
//		this(Objects.requireNonNull(parent).query(), parent, state);
//	}

	/**
	 * Avoid using this ctor directly
	 * 
	 * @param query
	 * @param state
	 */
	public FacetNodeImpl(FacetedQueryResource query, BgpNode state) {
		this.query = query;
//		this.parent = parent;
		this.state = Objects.requireNonNull(state); 
	}

	@Override
	public FacetNodeResource parent() {
		BgpNode p = Optional.ofNullable(state.parent()).map(BgpMultiNode::parent).orElse(null);
		
		return p == null ? null : new FacetNodeImpl(query, p);
	}
	
	@Override
	public FacetedQueryResource query() {
		return query;
	}
	
	@Override
	public BgpNode state() {
		return state;
	}
	
	@Override
	public FacetDirNode fwd() {
		return new FacetDirNodeImpl(this, state.fwd());
	}

	@Override
	public FacetDirNode bwd() {
		return new FacetDirNodeImpl(this, state.bwd());
	}
	
	
	@Override
	public BinaryRelation getReachingRelation() {
		BinaryRelation result = BgpNode.getReachingRelation(state);
		return result;
	}

//	@Override
//	public BinaryRelation getReachingRelation() {
//		BinaryRelation result;
//
//		FacetNodeResource parent = parent();
//		if(parent == null) {
//			result = null;
//		} else {
//			
//			boolean isReverse = false;
//			Set<Statement> set = ResourceUtils.listProperties(parent().state(), null).filterKeep(stmt -> stmt.getObject().equals(state)).toSet();
//			
//			if(set.isEmpty()) {
//				isReverse = true;
//				set = ResourceUtils.listReverseProperties(parent().state(), null).filterKeep(stmt -> stmt.getSubject().equals(state)).toSet();
//			}
//			
//			// TODO Should never fail - but ensure that
//			Property p = set.iterator().next().getPredicate();
//			
//			result = create(p.asNode(), isReverse);
//		}
//
//		return result;
//	}
//

	
	
	public boolean isReverse() {
		boolean isReverse = false;
		Set<Statement> set = ResourceUtils.listProperties(parent().state(), null).filterKeep(stmt -> stmt.getObject().equals(state)).toSet();
		
		if(set.isEmpty()) {
			isReverse = true;
//			set = ResourceUtils.listReverseProperties(parent().state(), null).filterKeep(stmt -> stmt.getSubject().equals(state)).toSet();
		}
		
		return isReverse;
	}
	

	public DataQuery<RDFNode> createValueQuery(boolean applySelfConstraints) {
		BgpNode bgpRoot = query.modelRoot().getBgpRoot();
		
		FacetedQueryGenerator<BgpNode> qgen = new FacetedQueryGenerator<BgpNode>(new PathAccessorImpl(bgpRoot));
		query.constraints().forEach(c -> qgen.addConstraint(c.expr()));

		BgpNode focus = query().focus().state();

		UnaryRelation c = qgen.getConceptForAtPath(focus, this.state, applySelfConstraints);
		
		//System.out.println("Available values: " + c);
		
		SparqlQueryConnection conn = query.connection();
		DataQuery<RDFNode> result = new DataQueryImpl<>(conn, c.getVar(), c.getElement(), null, RDFNode.class);

		return result;

	}

	@Override
	public DataQuery<RDFNode> availableValues() {
		DataQuery<RDFNode> result = createValueQuery(false);
		return result;		
	}

	@Override
	public DataQuery<RDFNode> remainingValues() {
		DataQuery<RDFNode> result = createValueQuery(true);
		return result;
	}

	public FacetNode as(String varName) {
		ResourceUtils.setLiteralProperty(state, Vocab.alias, varName);		
		return this;
	}
	
	@Override
	public FacetNode as(Var var) {
		return as(var.getName());
	}
	
	@Override
	public Var alias() {
		return ResourceUtils.tryGetLiteralPropertyValue(state, Vocab.alias, String.class)
			.map(Var::alloc).orElse(null);
	}

//	@Override
//	public FacetNodeResource parent() {
//		return parent;
//	}

	@Override
	public FacetNode root() {
		FacetNode result = TreeUtils.findRoot(this, FacetNode::parent);
		return result;
	}

	@Override
	public ConstraintFacade<? extends FacetNodeResource> constraints() {
		return new ConstraintFacadeImpl<FacetNodeResource>(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FacetNodeImpl other = (FacetNodeImpl) obj;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		return true;
	}

	@Override
	public String toString() {
		BgpNode bgpRoot = query.modelRoot().getBgpRoot();
		FacetedQueryGenerator<BgpNode> qgen = new FacetedQueryGenerator<>(new PathAccessorImpl(bgpRoot));
		query.constraints().forEach(c -> qgen.addConstraint(c.expr()));

		UnaryRelation c = qgen.getConceptForAtPath(this.query.focus().state(), this.state, false);
		return this.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this))
				+ "{" + c + "}";
	}

	@Override
	public FacetNode chRoot() {
		state.chRoot();

		FacetedQueryResource query = query();

		ResourceUtils.setProperty(query.modelRoot(), Vocab.root, state);
		return this;
	}

	@Override
	public FacetNode chFocus() {
		query.focus(this);
		return this;
	}
}
