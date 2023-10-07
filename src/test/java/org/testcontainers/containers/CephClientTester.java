package org.testcontainers.containers;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * Used to test intricate details about Ceph.
 * Start docker compose with docker compose up -d
 * Then run this main function to test if demo bucket exists
 */
public class CephClientTester {
    public static void main(String[] args) {
        AWSCredentials credentials = new BasicAWSCredentials(
                "demo",
                "demo"
        );
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                "http://localhost:8080",
                ""
        );
        AmazonS3 client = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
        boolean bucketExist = client.doesBucketExistV2("demo");
        System.out.println(bucketExist ? "SUCCESS": "Failed to find bucket");
    }
}
