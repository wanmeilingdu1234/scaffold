package com.rick.scaffold.search.services.worker;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.rick.scaffold.common.jsontool.JsonMapper;
import com.rick.scaffold.search.services.RZIndexKeywordRequest;
import com.rick.scaffold.search.services.delegate.SearchDelegate;
import com.rick.scaffold.search.services.field.RZBooleanField;
import com.rick.scaffold.search.services.field.RZDoubleField;
import com.rick.scaffold.search.services.field.RZField;
import com.rick.scaffold.search.services.field.RZIntegerField;
import com.rick.scaffold.search.services.field.RZListField;
import com.rick.scaffold.search.services.field.RZLongField;
import com.rick.scaffold.search.services.field.RZStringField;
import com.rick.scaffold.search.utils.CustomIndexConfiguration;
import com.rick.scaffold.search.utils.CustomIndexFieldConfiguration;
import com.rick.scaffold.search.utils.SearchClient;

public class KeywordIndexerImpl implements IndexWorker {

	@Autowired
	DeleteKeywordsImpl deleteKeywordsImpl;

	@Autowired
	private SearchDelegate searchDelegate;

	private static Logger log = Logger.getLogger(KeywordIndexerImpl.class);

	private static boolean init = false;

	private List<CustomIndexConfiguration> indexConfigurations = null;

	public List<CustomIndexConfiguration> getIndexConfigurations() {
		return indexConfigurations;
	}

	public void setIndexConfigurations(
			List<CustomIndexConfiguration> indexConfigurations) {
		this.indexConfigurations = indexConfigurations;
	}

	private static Map<String, CustomIndexConfiguration> indexConfigurationsMap = null;
	
	@Override
	public void init(SearchClient client) {
		if (!init) {
			init();
		}
	}

	private synchronized void init() {
		try {
			if (indexConfigurations != null) {
				for (CustomIndexConfiguration ic : indexConfigurations) {
					String key = ic.getOnType();
					if (indexConfigurationsMap == null) {
						indexConfigurationsMap = Maps.newHashMap();
                    }
					if (StringUtils.isBlank(key)) {
						log.debug("Require property createOnIndexName in keyword indexer");
						continue;
					}
					indexConfigurationsMap.put(key, ic);
				}
			}
			init = true;
		} catch (Exception e) {
			log.error("create keyword index fail.", e);
		}
	}

	@Override
	public void execute(SearchClient client, String json, String index,
			String type, Long id)
			throws Exception {
		if (!init) {
			init();
		}
		try {
			if (indexConfigurationsMap != null && indexConfigurationsMap.containsKey(type)) {
				CustomIndexConfiguration conf = indexConfigurationsMap.get(type);
				Map<String, Object> indexData = JsonMapper.fromJsonString(json, Map.class);
				// get fields to index
				List<CustomIndexFieldConfiguration> fields = conf.getFields();
				if (fields != null) {
					List<String> attrs = new ArrayList<String>();
					for (CustomIndexFieldConfiguration cifc : fields) {
						String fieldName = cifc.getFieldName();
						if (fieldName.trim().toLowerCase().equals("id")) {
							continue;
						}
						String fieldType = cifc.getFieldType();
						if (!StringUtils.isBlank(fieldType)) {
							if (fieldType.equals("List")) {
								try {
									List<String> keyWords = (List<String>) indexData.get(fieldName);
									if (keyWords != null) {
										attrs.addAll(keyWords);
									}
								} catch (Exception e) {// might have one instead
									String keyword = (String) indexData.get(fieldName);
									if (keyword != null) {
										attrs.add(keyword);
									}
								}
							} else {// String
								String keyword = (String) indexData.get(fieldName);
								if (keyword != null) {
									attrs.add(keyword);
								}
							}
						}// end field type
					}// end for
					if (attrs != null && attrs.size() > 0) {
						Collection<RZIndexKeywordRequest> bulks = Lists.newArrayList();
						for (String attr : attrs) {
							RZIndexKeywordRequest kr = new RZIndexKeywordRequest();
							if (StringUtils.isBlank(attr)) {
								continue;
							}
							kr.setId(id);
							kr.setKey(attr);
							bulks.add(kr);
						}
						// delete previous keywords for the same id
						deleteKeywordsImpl.deleteObject(client, index, conf.getTypeName(), id);
						searchDelegate.bulkIndexKeywords(bulks, index, conf.getTypeName());
					}
				}
			}
		} catch (Exception e) {
			log.error("Cannot index keywords, maybe a timing ussue for no shards available", e);
		}
	}

}
