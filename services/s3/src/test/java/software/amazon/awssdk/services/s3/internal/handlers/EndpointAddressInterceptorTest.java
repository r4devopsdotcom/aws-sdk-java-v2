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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.awscore.AwsExecutionAttribute.AWS_REGION;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.utils.InterceptorTestUtils;

public class EndpointAddressInterceptorTest {

    private static final String AP_ARN = "arn:aws:s3:us-west-2:123456789012:accesspoint:foobar";
    private static final String OUTPOSTS_ARN = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456"
                                               + ":accesspoint:myaccesspoint";
    private static final String OBJECT_LAMBDA_ARN = "arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/myol";
    private static final String KEY = "test-key";
    private static final String DEFAULT_SIGNING_NAME = "s3";
    private static final String OUTPOSTS_SIGNING_NAME = "s3-outposts";
    private static final String OBJECT_LAMBDA_SIGNING_NAME = "s3-object-lambda";
    private static final Region DEFAULT_REGION = Region.US_WEST_2;

    private EndpointAddressInterceptor interceptor;

    @BeforeEach
    public void setUp() throws Exception {
        interceptor = new EndpointAddressInterceptor();
    }

    @Test
    public void accesspointArn_shouldReturnStandardRequest() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder(), DEFAULT_REGION);
        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(createContext(AP_ARN), executionAttributes);

        String expectedEndpoint = "http://foobar-123456789012.s3-accesspoint.us-west-2.amazonaws.com";
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(uri(expectedEndpoint));
        assertThat(executionAttributes.getAttribute(SIGNING_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo(DEFAULT_SIGNING_NAME);
    }

    @Test
    public void outpostAccessPointArn_sameRegion_shouldRegion() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder(), DEFAULT_REGION);
        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(createContext(OUTPOSTS_ARN), executionAttributes);

        String expectedEndpoint = "http://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-west-2.amazonaws.com";
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(uri(expectedEndpoint));
        assertThat(executionAttributes.getAttribute(SIGNING_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo(OUTPOSTS_SIGNING_NAME);
    }

    @Test
    public void outpostAccessPointArn_crossRegion_ArnRegionEnabled_correctlyInfersPartition() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(true),
                                                                            Region.US_EAST_1);
        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(createContext(OUTPOSTS_ARN), executionAttributes);

        String expectedEndpoint = "http://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-west-2.amazonaws.com";
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(uri(expectedEndpoint));
        assertThat(executionAttributes.getAttribute(SIGNING_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo(OUTPOSTS_SIGNING_NAME);
    }

    @Test
    public void objectLambdaAccessPointArn_sameRegion_shouldRegion() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder(), DEFAULT_REGION);
        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(createContext(OBJECT_LAMBDA_ARN), executionAttributes);

        String expectedEndpoint = "http://myol-123456789012.s3-object-lambda.us-west-2.amazonaws.com";
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(uri(expectedEndpoint));
        assertThat(executionAttributes.getAttribute(SIGNING_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo(OBJECT_LAMBDA_SIGNING_NAME);
    }

    @Test
    public void objectLambdaAccessPointArn_crossRegion_ArnRegionEnabled_correctlyInfersPartition() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(true),
                Region.US_EAST_1);
        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(createContext(OBJECT_LAMBDA_ARN), executionAttributes);

        String expectedEndpoint = "http://myol-123456789012.s3-object-lambda.us-west-2.amazonaws.com";
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(uri(expectedEndpoint));
        assertThat(executionAttributes.getAttribute(SIGNING_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo(OBJECT_LAMBDA_SIGNING_NAME);
    }

    private Context.ModifyHttpRequest createContext(String accessPointArn) {
        URI customUri = URI.create(String.format("http://s3-test.com/%s/%s", urlEncode(accessPointArn), KEY));
        PutObjectRequest request = PutObjectRequest.builder().bucket(accessPointArn).key(KEY).build();

        return InterceptorTestUtils.modifyHttpRequestContext(request, InterceptorTestUtils.sdkHttpRequest(customUri));
    }

    private ExecutionAttributes createExecutionAttributes(S3Configuration.Builder builder, Region region) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SERVICE_CONFIG, builder.build());
        executionAttributes.putAttribute(AWS_REGION, region);
        executionAttributes.putAttribute(SIGNING_REGION, region);
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, DEFAULT_SIGNING_NAME);
        return executionAttributes;
    }

    private URI uri(String expectedEndpoint) {
        return URI.create(String.format("%s/%s", expectedEndpoint, KEY));
    }


    @Test
    public void objectLambdaAccessPointArn_usEast_and_region_s3External1_throwsIllegalArgumentException() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(false),
                Region.of("s3-external-1"));
        assertThatThrownBy(() -> interceptor.modifyHttpRequest(createContext("arn:aws:s3:us-east-1:123456789012:accesspoint:myol"), executionAttributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("region");

    }

    @Test
    public void objectLambdaAccessPointArn_usEast1_and_region_awsGlobalawsGlobal_throwsIllegalArgumentException() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(false),
                Region.AWS_GLOBAL);
        assertThatThrownBy(() -> interceptor.modifyHttpRequest(createContext("arn:aws:s3:us-east-1:123456789012:accesspoint:myol"), executionAttributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("region");
    }

    @Test
    public void objectLambdaAccessPointArn_usGovEast1_and_region_in_fips_useArnRegionFalse() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(false),
                Region.of("fips-us-gov-east-1"));
        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(createContext(
                "arn:aws:s3-object-lambda:us-gov-east-1:123456789012:accesspoint/myol"), executionAttributes);
        String expectedEndpoint = "http://myol-123456789012.s3-object-lambda-fips.us-gov-east-1.amazonaws.com";
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(uri(expectedEndpoint));
        assertThat(executionAttributes.getAttribute(SIGNING_REGION)).isEqualTo(Region.US_GOV_EAST_1);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo(OBJECT_LAMBDA_SIGNING_NAME);
    }

    @Test
    public void objectLambdaAccessPointArn_usGovEast1_and_region_in_fips_useArnRegionTrue() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(true),
                Region.of("fips-us-gov-east-1"));
        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(createContext(
                "arn:aws:s3-object-lambda:us-gov-east-1:123456789012:accesspoint/myol"), executionAttributes);
        String expectedEndpoint = "http://myol-123456789012.s3-object-lambda-fips.us-gov-east-1.amazonaws.com";
        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(uri(expectedEndpoint));
        assertThat(executionAttributes.getAttribute(SIGNING_REGION)).isEqualTo(Region.US_GOV_EAST_1);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo(OBJECT_LAMBDA_SIGNING_NAME);
    }

    @Test
    public void objectLambdaAccessPointArn_usGovWest1_and_clientRegion_fips_usEast1_useArnRegionFalse_throwsIllegalArgumentException() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(false),
                Region.of("fips-us-gov-east-1"));

        assertThatThrownBy(() -> interceptor.modifyHttpRequest(createContext(
                "arn:aws:s3-object-lambda:us-gov-west-1:123456789012:accesspoint/myol"), executionAttributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cross region access not allowed for fips region in client or arn.");
     }

    @Test
    public void objectLambdaAccessPointArn_usGovWest1_and_clientRegion_fips_usEast1_useArnRegionTrue_throwsIllegalArgumentException() {
        ExecutionAttributes executionAttributes = createExecutionAttributes(S3Configuration.builder().useArnRegionEnabled(true),
                Region.of("fips-us-gov-east-1"));
        assertThatThrownBy(() -> interceptor.modifyHttpRequest(createContext(
                "arn:aws:s3-object-lambda:us-gov-west-1:123456789012:accesspoint/myol"), executionAttributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cross region access not allowed for fips region in client or arn");
    }

}
