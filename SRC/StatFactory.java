package com.apelon.modules.dts.editor.namespaceplugin;

import java.util.ArrayList;
import java.util.HashMap;

import com.apelon.apelonserver.client.ApelonException;
import com.apelon.beans.dts.plugin.DTSAppManager;
import com.apelon.dts.client.DTSQuery;
import com.apelon.dts.client.ServerConnectionEJB;
import com.apelon.dts.client.association.AssociationType;
import com.apelon.dts.client.attribute.DTSProperty;
import com.apelon.dts.client.attribute.DTSPropertyType;
import com.apelon.dts.client.attribute.DTSRoleType;
import com.apelon.dts.client.attribute.Kind;
import com.apelon.dts.client.attribute.QualifierType;
import com.apelon.dts.client.concept.ConceptAttributeSetDescriptor;
import com.apelon.dts.client.concept.DTSSearchOptions;
import com.apelon.dts.client.namespace.ContentVersion;
import com.apelon.dts.client.namespace.Namespace;
import com.apelon.dts.client.namespace.NamespaceAttributeSetDescriptor;
import com.apelon.dts.client.namespace.NamespaceType;
import com.apelon.dts.client.term.TermAttributeSetDescriptor;
import com.apelon.dts.client.term.TermSearchOptions;

/**
 * get attribute statistics for Namespace Stats
 * <p>
 * Copyright (c) 2020 Apelon, Inc. All rights reserved.
 * @since 4.7.1
 */

public class StatFactory {
	
	public enum Attribute { BASIC, NAMESPACE_PROPS, AUTHORITY_PROPS, VERSION_PROPS, CONCEPT_PROPS, 
				CONCEPT_SYNONYMS, CONCEPT_ROLES, CONCEPT_INV_ROLES, CONCEPT_KINDS, CONCEPT_DEF_ROLES, 
				CONCEPT_ASSNS, CONCEPT_INV_ASSNS, TERM_PROPS, TERM_INV_SYNONYMS, TERM_ASSNS, TERM_INV_ASSNS };
	
	public static class StatObj {
		
		String value;
		long localCount;
		long allCount;
		
		StatObj(String str, long cnt) {
			this(str, cnt, -1);
		}
		
		StatObj(String str, long lcnt, long acnt) {
			value = str;
			localCount = lcnt;
			allCount = acnt;
		}
	}
	
	StatFactory() { }
	
	
	static HashMap<Attribute,ArrayList<StatObj>> getStats(int nsid, boolean doAll) throws Exception {
		long lcnt;		//count of local instances
		long acnt;		//count of all instances
		
		ArrayList<StatObj> basic = new ArrayList<StatObj>();
		ArrayList<StatObj> space_props = new ArrayList<StatObj>();
		ArrayList<StatObj> version_props = new ArrayList<StatObj>();
		ArrayList<StatObj> concept_props = new ArrayList<StatObj>();
		ArrayList<StatObj> term_props = new ArrayList<StatObj>();
		ArrayList<StatObj> concept_assns = new ArrayList<StatObj>();
		ArrayList<StatObj> concept_invAssns = new ArrayList<StatObj>();
		ArrayList<StatObj> term_assns = new ArrayList<StatObj>();
		ArrayList<StatObj> term_invAssns = new ArrayList<StatObj>();
		ArrayList<StatObj> concept_synonyms = new ArrayList<StatObj>();
		ArrayList<StatObj> term_invSynonyms = new ArrayList<StatObj>();
		ArrayList<StatObj> concept_roles = new ArrayList<StatObj>();
		ArrayList<StatObj> concept_invRoles = new ArrayList<StatObj>();
		ArrayList<StatObj> concept_kinds = new ArrayList<StatObj>();
		
		DTSSearchOptions localCopts = new DTSSearchOptions(Integer.MAX_VALUE-1, nsid, ConceptAttributeSetDescriptor.NO_ATTRIBUTES);
		DTSSearchOptions allCopts = new DTSSearchOptions(Integer.MAX_VALUE-1, DTSSearchOptions.ALL_NAMESPACES, ConceptAttributeSetDescriptor.NO_ATTRIBUTES);
		TermSearchOptions localTopts = new TermSearchOptions(Integer.MAX_VALUE-1, nsid, TermAttributeSetDescriptor.NO_ATTRIBUTES);
		TermSearchOptions allTopts = new TermSearchOptions(Integer.MAX_VALUE-1, TermSearchOptions.ALL_NAMESPACES, TermAttributeSetDescriptor.NO_ATTRIBUTES);
		DTSQuery query = DTSAppManager.getQuery();
		try {
			acnt = -1;
			
			Namespace space = query.getNamespaceQuery().findNamespaceById(nsid, NamespaceAttributeSetDescriptor.ALL_ATTRIBUTES);
			boolean isOntylog = space.getNamespaceType().equals(NamespaceType.ONTYLOG) || 
									space.getNamespaceType().equals(NamespaceType.ONTYLOG_EXTENSION);
			//get concepts
			lcnt = query.getSearchQuery().countConceptsWithNameMatching("*", localCopts);
			basic.add(new StatObj("Concepts", lcnt));
			lcnt = query.getTermSearchQuery().countTermsWithNameMatching("*", localTopts);
			basic.add(new StatObj("Terms", lcnt));
			basic.add(new StatObj("Versions",space.getContentVersions().length));
			//analyze namespace
			DTSPropertyType[] ptypes = query.getNamespaceQuery().getNamespacePropertyTypes(nsid);
			for (DTSPropertyType type : ptypes) {
				space_props.add(new StatObj(type.getName(), 0));	//initialize all
			}
			for (DTSProperty prop : space.getProperties()) {
				incrementCount(space_props, prop.getName());		//count this prop
			}
			//now versions, count "versions with ..."
			ptypes = query.getNamespaceQuery().getVersionPropertyTypes(nsid);
			ArrayList<String> propList = new ArrayList<String>();	//list of property types seen in a version
			for (DTSPropertyType type : ptypes) {
				version_props.add(new StatObj(type.getName(), 0));	//initialize all
			}
			for (ContentVersion version : space.getContentVersions()) {
				propList.clear();									//clear the list
				for (DTSProperty prop : version.getProperties()) {
					if (!propList.contains(prop.getName())) {
						incrementCount(version_props, prop.getName());
						propList.add(prop.getName());		//mark this version counted
					}
				}
			}
			//handle properties, concept
			ptypes = query.getDTSConceptQuery().getConceptPropertyTypes(nsid);
			for (DTSPropertyType type : ptypes) {
				lcnt = query.getSearchQuery().countConceptsWithPropertyMatching(type, "*", localCopts);
				if (doAll) acnt = query.getSearchQuery().countConceptsWithPropertyMatching(type, "*", allCopts);
				concept_props.add(new StatObj(type.getName(), lcnt, acnt));
			}
			//term props
			ptypes = query.getTermSearchQuery().getTermPropertyTypes(nsid);
			for (DTSPropertyType type : ptypes) {
				lcnt = query.getTermSearchQuery().countTermsWithPropertyMatching(type, "*", localTopts);
				if (doAll) acnt = query.getTermSearchQuery().countTermsWithPropertyMatching(type, "*", allTopts);
				term_props.add(new StatObj(type.getName(), lcnt, acnt));
			}
			//handle associations
			AssociationType atypes[] = query.getAssociationQuery().getConceptAssociationTypes(nsid);
			for (AssociationType type : atypes) {
				lcnt = query.getSearchQuery().countConceptsWithConceptAssociationMatching(type, "*", localCopts);
				if (doAll) acnt = query.getSearchQuery().countConceptsWithConceptAssociationMatching(type, "*", allCopts);
				concept_assns.add(new StatObj(type.getName(), lcnt, acnt));
				lcnt = query.getSearchQuery().countConceptsWithInverseConceptAssociationMatching(type, "*", localCopts);
				if (doAll) acnt = query.getSearchQuery().countConceptsWithInverseConceptAssociationMatching(type, "*", allCopts);
				concept_invAssns.add(new StatObj(type.getName(), lcnt, acnt));
			}
			atypes = query.getAssociationQuery().getSynonymTypes(nsid);
			for (AssociationType type : atypes) {
				lcnt = query.getSearchQuery().countConceptsWithSynonymMatching(type, "*", localCopts);
				if (doAll) acnt = query.getSearchQuery().countConceptsWithSynonymMatching(type, "*", allCopts);
				concept_synonyms.add(new StatObj(type.getName(), lcnt, acnt));
				lcnt = query.getTermSearchQuery().countTermsWithInverseSynonymMatching(type, "*", localTopts);
				if (doAll) acnt = query.getTermSearchQuery().countTermsWithInverseSynonymMatching(type, "*", allTopts);
				term_invSynonyms.add(new StatObj(type.getName(), lcnt, acnt));
			}
			atypes = query.getAssociationQuery().getTermAssociationTypes(nsid);
			for (AssociationType type : atypes) {
				lcnt = query.getTermSearchQuery().countTermsWithTermAssociationMatching(type, "*", localTopts);
				if (doAll) acnt = query.getTermSearchQuery().countTermsWithTermAssociationMatching(type, "*", allTopts);
				term_assns.add(new StatObj(type.getName(), lcnt, acnt));
				lcnt = query.getTermSearchQuery().countTermsWithInverseTermAssociationMatching(type, "*", localTopts);
				if (doAll) acnt = query.getTermSearchQuery().countTermsWithInverseTermAssociationMatching(type, "*", allTopts);
				term_invAssns.add(new StatObj(type.getName(), lcnt, acnt));
			}
			//do roles if ontylog
			if (isOntylog) {
				DTSRoleType rtypes[] = query.getOntylogConceptQuery().getRoleTypes(nsid);
				for (DTSRoleType type : rtypes) {
					lcnt = query.getSearchQuery().countConceptsWithRoleMatching(type, "*", localCopts);
					if (doAll) acnt = query.getSearchQuery().countConceptsWithRoleMatching(type, "*", allCopts);
					concept_roles.add(new StatObj(type.getName(), lcnt, acnt));
					lcnt = query.getSearchQuery().countConceptsWithInverseRoleMatching(type, "*", localCopts);
					if (doAll) acnt = query.getSearchQuery().countConceptsWithInverseRoleMatching(type, "*", allCopts);
					concept_invRoles.add(new StatObj(type.getName(), lcnt, acnt));
				}
				//do kinds
				Kind kinds[] = query.getOntylogConceptQuery().getKinds(nsid);
				for (Kind kind : kinds) {
					lcnt = query.getSearchQuery().countConceptsWithKind(kind, localCopts);
					concept_kinds.add(new StatObj(kind.getName(), lcnt));
				}
			}
			//now qualifiers
			/*
			QualifierType qtypes[] = query.getNamespaceQuery().getConceptPropertyQualifierTypes(nsid);
			for (QualifierType type : qtypes) {
				cnt = query.getSearchQuery().countConceptsWithPropertyQualifierMatching(type, "*", sopts);
				concept_prop_quals.put(type.getName(), new StatObj(type.getName(), cnt));
			}*/
			
			HashMap<Attribute,ArrayList<StatObj>> map = new HashMap<Attribute,ArrayList<StatObj>>();
			map.put(Attribute.BASIC, basic);
			map.put(Attribute.NAMESPACE_PROPS, space_props);
			map.put(Attribute.VERSION_PROPS, version_props);
			map.put(Attribute.CONCEPT_PROPS, concept_props);
			map.put(Attribute.CONCEPT_SYNONYMS, concept_synonyms);
			map.put(Attribute.TERM_INV_SYNONYMS, term_invSynonyms);
			if (isOntylog) {
				map.put(Attribute.CONCEPT_KINDS,  concept_kinds);
				map.put(Attribute.CONCEPT_ROLES,  concept_roles);
				map.put(Attribute.CONCEPT_INV_ROLES,  concept_invRoles);
			}
			map.put(Attribute.CONCEPT_ASSNS, concept_assns);
			map.put(Attribute.CONCEPT_INV_ASSNS, concept_invAssns);
			map.put(Attribute.TERM_PROPS, term_props);
			map.put(Attribute.TERM_ASSNS, term_assns);
			map.put(Attribute.TERM_INV_ASSNS, term_invAssns);
			return map;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception ("Error accessing Namespace attributes");
		}
	}
	
	//increment the statObj associated with name in the designated list
	//assumes the StatObj exists
	private static void incrementCount(ArrayList<StatObj> list, String name) {
		for (StatObj obj : list) {
			if (obj.value.equals(name)) {
				obj.localCount++;
				break;
			}
		}
	}
	
	/********************************************************/
	private static ServerConnectionEJB getConnection(String host, int port, String instance, String user, String pass) throws ApelonException {
		// Pass parameters to constructor 
		return new ServerConnectionEJB(host, port, instance, user, pass);
	}

	/********************************************************/

    public static void main(String[] args) {
		ServerConnectionEJB conn = null;
		String dtsUser, dtsPass, dtsHost, dtsInstance;
		int dtsPort;

		try {
			dtsUser = "dtsadminuser";
			dtsPass = "dtsadmin";
			dtsInstance = "dtsjboss";
			dtsHost = "localhost";
			dtsPort = 7447;

			conn = getConnection(dtsHost, dtsPort, dtsInstance, dtsUser, dtsPass);
			DTSAppManager.getQuery().setConnection(conn);
			System.out.println("Connected to "+dtsHost+"/"+dtsInstance+"\n");
			
			//int nsid = 30;		//SNOMED
			//int nsid = 20;		//CPT
			//int nsid = 32777;	//State of the Union
			int nsid = 50;		//Triad
			/*
        	Namespace[] spaces = DTSAppManager.getQuery().getNamespaceQuery().getNamespaces();
        	for (Namespace space : spaces) {
        		System.out.println(space.getName()+" "+space.getId());
        	} */
        	HashMap<Attribute,ArrayList<StatObj>> maps = StatFactory.getStats(nsid, false);
        	for (Attribute cat : maps.keySet()) {
        		System.out.println(cat);
        		ArrayList<StatObj> list = maps.get(cat);
        		for (StatObj obj : list) {
        			System.out.println("   "+obj.value+"="+obj.localCount+" "+
        								(obj.allCount<0?"":"("+(obj.allCount-obj.localCount)+")"));
        		}
        		System.out.println();
        	}
        } catch (Exception e) {
        	System.out.println(e.getMessage());
        }
        
    }
}
