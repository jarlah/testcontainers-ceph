package org.testcontainers.containers;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SSECustomerKey;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * Used to test intricate details about Ceph.
 * Start docker compose with docker compose up -d
 * Then run this main function to test if demo bucket exists
 */
public class CephClientTester {
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");

        Key key = getPasswordBasedKey("AES", 256, "testtesttest".toCharArray());

        AWSCredentials credentials = new BasicAWSCredentials(
                "demo",
                "demo"
        );
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                "https://ceph-demo:8443",
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

        URL file = CephClientTester.class.getResource("/object_to_upload.txt");
        assert file != null;
        PutObjectRequest request = new PutObjectRequest("demo", "my-objectname", file.getFile());
        request.setSSECustomerKey(new SSECustomerKey(key.getEncoded()));
        client.putObject(request);
    }

    private static Key getPasswordBasedKey(String cipher, int keySize, char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = new byte[100];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 1000, keySize);
        SecretKey pbeKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pbeKeySpec);
        return new SecretKeySpec(pbeKey.getEncoded(), cipher);
    }
}
