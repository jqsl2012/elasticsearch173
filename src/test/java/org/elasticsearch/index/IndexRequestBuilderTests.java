/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.stresstest.indexing.BulkIndexingStressTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.TEST,numDataNodes =2)
public class IndexRequestBuilderTests extends ElasticsearchIntegrationTest {
    
	@Test
	public void testBulk(){
		createIndex("bulk");
        ensureYellow();
		BulkRequest bulk = new BulkRequest();
//		BulkRequestBuilder bulk = client().prepareBulk();
        for(int i = 0; i < 10 ; i++){
        	IndexRequestBuilder request = client().prepareIndex("bulk", "bulk")
            		.setSource(new BytesArray("{\"test_field\" : \"foobar"+i+"\"}"))
            		.setId(Strings.base64UUID())
            		.setOpType(OpType.BULK);
            bulk.add(request.request());
        }

        client().bulk(bulk,new ActionListener<BulkResponse>() {
			
			@Override
			public void onResponse(BulkResponse response) {
				System.out.println("onResponse");
			}
			
			@Override
			public void onFailure(Throwable e) {
				System.out.println("onFailure");
			}
		});
        
        try {
			TimeUnit.DAYS.sleep(1);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	
    @Test
    public void testSetSource() throws InterruptedException, ExecutionException {
        createIndex("test");
        ensureYellow();
        Map<String, Object> map = new HashMap<>();
        map.put("test_field", "foobar");
        IndexRequestBuilder[] builders = new IndexRequestBuilder[] {
                client().prepareIndex("test", "test").setSource((Object)"test_field", (Object)"foobar"),
                client().prepareIndex("test", "test").setSource("{\"test_field\" : \"foobar\"}"),
                client().prepareIndex("test", "test").setSource(new BytesArray("{\"test_field\" : \"foobar\"}")),
                client().prepareIndex("test", "test").setSource(new BytesArray("{\"test_field\" : \"foobar\"}")),
                client().prepareIndex("test", "test").setSource(new BytesArray("{\"test_field\" : \"foobar\"}").toBytes()),
                client().prepareIndex("test", "test").setSource(map)
        };

        indexRandom(true, builders);
        
//        TimeUnit.DAYS.sleep(1);
        SearchResponse searchResponse = client().prepareSearch("test").setQuery(QueryBuilders.termQuery("test_field", "foobar")).get();
        ElasticsearchAssertions.assertHitCount(searchResponse, builders.length);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testOddNumberOfSourceObjetc() {
        client().prepareIndex("test", "test").setSource((Object)"test_field", (Object)"foobar", new Object());
    }

}
