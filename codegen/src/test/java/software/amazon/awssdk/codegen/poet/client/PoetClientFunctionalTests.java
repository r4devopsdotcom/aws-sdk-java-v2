/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class PoetClientFunctionalTests {
    @Test
    public void asyncClientClass() throws Exception {
        AsyncClientClass asyncClientClass = createAsyncClientClass(ClientTestModels.restJsonServiceModels());
        assertThat(asyncClientClass, generatesTo("test-async-client-class.java"));
    }

    @Test
    public void asyncClientInterface() throws Exception {
        ClassSpec asyncClientInterface = new AsyncClientInterface(ClientTestModels.restJsonServiceModels());
        assertThat(asyncClientInterface, generatesTo("test-json-async-client-interface.java"));
    }

    @Test
    public void delegatingAsyncClientClass() throws Exception {
        DelegatingAsyncClientClass asyncClientDecoratorAbstractClass =
            new DelegatingAsyncClientClass(ClientTestModels.restJsonServiceModels());
        assertThat(asyncClientDecoratorAbstractClass, generatesTo("test-abstract-async-client-class.java"));
    }

    @Test
    public void simpleMethodsIntegClass() throws Exception {
        ClientSimpleMethodsIntegrationTests simpleMethodsClass = new ClientSimpleMethodsIntegrationTests(
            ClientTestModels.restJsonServiceModels());
        assertThat(simpleMethodsClass, generatesTo("test-simple-methods-integ-class.java"));
    }

    @Test
    public void syncClientClassRestJson() throws Exception {
        SyncClientClass syncClientClass = createSyncClientClass(ClientTestModels.restJsonServiceModels());
        assertThat(syncClientClass, generatesTo("test-json-client-class.java"));
    }

    @Test
    public void syncClientClassQuery() throws Exception {
        SyncClientClass syncClientClass = createSyncClientClass(ClientTestModels.queryServiceModels());
        assertThat(syncClientClass, generatesTo("test-query-client-class.java"));
    }

    @Test
    public void asyncClientClassAwsJson() throws Exception {
        AsyncClientClass asyncClientClass = createAsyncClientClass(ClientTestModels.awsJsonServiceModels());
        assertThat(asyncClientClass, generatesTo("test-aws-json-async-client-class.java"));
    }

    @Test
    public void asyncClientClassQuery() throws Exception {
        AsyncClientClass syncClientClass = createAsyncClientClass(ClientTestModels.queryServiceModels());
        assertThat(syncClientClass, generatesTo("test-query-async-client-class.java"));
    }

    @Test
    public void syncClientClassXml() throws Exception {
        SyncClientClass syncClientClass = createSyncClientClass(ClientTestModels.xmlServiceModels());
        assertThat(syncClientClass, generatesTo("test-xml-client-class.java"));
    }

    @Test
    public void asyncClientClassXml() throws Exception {
        AsyncClientClass syncClientClass = createAsyncClientClass(ClientTestModels.xmlServiceModels());
        assertThat(syncClientClass, generatesTo("test-xml-async-client-class.java"));
    }

    private SyncClientClass createSyncClientClass(IntermediateModel model) {
        return new SyncClientClass(GeneratorTaskParams.create(model, "sources/", "tests/"));
    }

    private AsyncClientClass createAsyncClientClass(IntermediateModel model) {
        return new AsyncClientClass(GeneratorTaskParams.create(model, "sources/", "tests/"));
    }

    @Test
    public void syncClientInterface() throws Exception {
        ClassSpec syncClientInterface = new SyncClientInterface(ClientTestModels.restJsonServiceModels());
        assertThat(syncClientInterface, generatesTo("test-json-client-interface.java"));
    }

    @Test
    public void syncClientEndpointDiscovery() throws Exception {
        ClassSpec syncClientEndpointDiscovery = createSyncClientClass(ClientTestModels.endpointDiscoveryModels());
        assertThat(syncClientEndpointDiscovery, generatesTo("test-endpoint-discovery-sync.java"));
    }

    @Test
    public void asyncClientEndpointDiscovery() throws Exception {
        ClassSpec asyncClientEndpointDiscovery = new AsyncClientClass(
            GeneratorTaskParams.create(ClientTestModels.endpointDiscoveryModels(), "sources/", "tests/"));
        assertThat(asyncClientEndpointDiscovery, generatesTo("test-endpoint-discovery-async.java"));
    }

    @Test
    public void asyncClientCustomServiceMetaData() throws Exception {
        ClassSpec asyncClientCustomServiceMetaData = new AsyncClientClass(
            GeneratorTaskParams.create(ClientTestModels.customContentTypeModels(), "sources/", "tests/"));
        assertThat(asyncClientCustomServiceMetaData, generatesTo("test-customservicemetadata-async.java"));
    }

    @Test
    public void syncClientCustomServiceMetaData() throws Exception {
        ClassSpec syncClientCustomServiceMetaData = createSyncClientClass(ClientTestModels.customContentTypeModels());
        assertThat(syncClientCustomServiceMetaData, generatesTo("test-customservicemetadata-sync.java"));
    }

}
