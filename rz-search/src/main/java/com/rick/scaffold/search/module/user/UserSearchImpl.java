package com.rick.scaffold.search.module.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rick.scaffold.search.SearchConstants;
import com.rick.scaffold.search.services.RZEntry;
import com.rick.scaffold.search.services.RZFacet;
import com.rick.scaffold.search.services.RZSearchHit;
import com.rick.scaffold.search.services.RZSearchRequest;
import com.rick.scaffold.search.services.RZSearchResponse;
import com.rick.scaffold.search.services.RZSearchService;
import com.rick.scaffold.soa.search.SearchEntry;
import com.rick.scaffold.soa.search.SearchFacet;
import com.rick.scaffold.soa.search.SearchKeywords;
import com.rick.scaffold.soa.search.SearchResult;
import com.rick.scaffold.soa.search.model.IndexUser;
import com.rick.scaffold.soa.search.service.UserSearch;

@Service("userSearchService")
public class UserSearchImpl implements UserSearch {

	private static final Logger logger = LoggerFactory
			.getLogger(UserSearchImpl.class);

	private final static String TYPE = "user";

	@Autowired
	private RZSearchService searchService;

	@Override
	public void createIndex(IndexUser user) {
		String indice = SearchConstants.indice;
		try {
			searchService.index(user, indice, TYPE);
		} catch (Exception e) {
			logger.error("Create index for user fail.", e);
		}
	}

	@Override
	public void deleteIndex(Long id) {
		String collectionName = SearchConstants.indice;
		try {
			searchService.deleteObject(collectionName, TYPE, id);
		} catch (Exception e) {
			logger.error("Delete index for user fail.", e);
		}
	}

	@Override
	public SearchKeywords searchForKeywords(String index, String type,
			String jsonString, int entriesCount) {
		try {
			RZSearchResponse response = searchService.searchAutoComplete(
					index, jsonString, type, entriesCount);
			SearchKeywords keywords = new SearchKeywords();
			keywords.setKeywords(Arrays.asList(response.getInlineSearchList()));
			return keywords;
		} catch (Exception e) {
			logger.error("Error while searching keywords " + jsonString, e);
			return null;
		}
	}

	@Override
	public SearchResult search(String jsonString, int entriesCount,
			int startIndex) {
		try {
			String collectionName = SearchConstants.indice;
			RZSearchRequest request = new RZSearchRequest();
			request.setIndex(collectionName);
			request.setSize(entriesCount);
			request.setStart(startIndex);
			request.setType("user");
			request.setJson(jsonString);

			RZSearchResponse response = searchService.search(request);
			SearchResult resp = new SearchResult();
			resp.setTotalCount(response.getCount());
			List<SearchEntry> entries = new ArrayList<SearchEntry>();
			Collection<RZSearchHit> hits = response.getSearchHits();
			for (RZSearchHit hit : hits) {
				SearchEntry entry = new SearchEntry();
				Map<String, Object> metaEntries = hit.getMetaEntries();
				IndexUser indexUser = new IndexUser();
				Map sourceEntries = (Map) metaEntries.get("source");
				indexUser.setEmail((String) sourceEntries.get("email"));
				indexUser.setId(Long.valueOf(sourceEntries.get("id").toString()));
				indexUser.setLoginName((String) sourceEntries.get("loginName"));
				indexUser.setName((String) sourceEntries.get("name"));
				indexUser.setPhone((String) sourceEntries.get("phone"));
				entry.setIndexUser(indexUser);
				entries.add(entry);

				Map<String, HighlightField> fields = hit.getHighlightFields();
				if (fields != null) {
					List<String> highlights = new ArrayList<String>();
					for (HighlightField field : fields.values()) {
						Text[] text = field.getFragments();
						// text[0]
						String f = field.getName();
						highlights.add(f);
					}
					entry.setHighlights(highlights);
				}
			}
			resp.setEntries(entries);

			Map<String, RZFacet> facets = response.getFacets();
			if (facets != null) {
				Map<String, List<SearchFacet>> searchFacets = new HashMap<String, List<SearchFacet>>();
				for (String key : facets.keySet()) {
					RZFacet f = facets.get(key);
					List<SearchFacet> fs = searchFacets.get(key);
					if (fs == null) {
						fs = new ArrayList<SearchFacet>();
						searchFacets.put(key, fs);
					}
					List<RZEntry> facetEntries = f.getEntries();
					for (RZEntry facetEntry : facetEntries) {
						SearchFacet searchFacet = new SearchFacet();
						searchFacet.setKey(facetEntry.getName());
						searchFacet.setName(facetEntry.getName());
						searchFacet.setCount(f.getEntries().size());
						fs.add(searchFacet);
					}
				}
				resp.setFacets(searchFacets);
			}
			return resp;
		} catch (Exception e) {
			logger.error("Error while searching keywords " + jsonString, e);
			return null;
		}
	}
}
