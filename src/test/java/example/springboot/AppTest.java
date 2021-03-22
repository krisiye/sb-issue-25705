package example.springboot;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.vault.VaultContainer;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;

import example.testcontainers.consul.ConsulConfiguration;
import example.testcontainers.consul.ConsulConfiguration.ACL;
import example.testcontainers.consul.ConsulConfiguration.Ports;
import example.testcontainers.consul.ConsulConfiguration.Tokens;
import example.testcontainers.consul.ConsulContainer;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {})
@ActiveProfiles("test")
public class AppTest
{
	
	static ConsulContainer cc;
	static VaultContainer vaultContainer;
	static  ConsulConfiguration config;
	
    @BeforeClass
    public static void init() throws IOException, InterruptedException {
	
		config = new ConsulConfiguration();
		Ports ports = new Ports();
	    ports.setHttpPort(8501);
		config.setPorts(ports);
		config.setDatacenter("default");
		
		String consulMasterToken = UUID.randomUUID().toString();
		String consulDefaultToken = UUID.randomUUID().toString();
		String consulAgentToken = UUID.randomUUID().toString();
		String consulReplicationToken = UUID.randomUUID().toString();
		
		Tokens tokens = new Tokens();
	    tokens.setMaster(consulMasterToken);
	    tokens.setDefaultToken(consulDefaultToken);
	    tokens.setAgent(consulAgentToken);
	    tokens.setReplication(consulReplicationToken);
	
	    ACL acl = new ACL();
	    acl.setEnabled(true);
	    acl.setDefaultPolicy("deny");
	    acl.setTokens(tokens);
	    
	    config.setAcl(acl);
	    
	    cc = new ConsulContainer(config);
	    cc.start();

	    System.setProperty("spring.cloud.consul.host", "127.0.0.1");
	    System.setProperty("spring.cloud.consul.port", String.valueOf(cc.getMappedPort(cc.getHttpPort())));
        ConsulClient client = new ConsulClient("127.0.0.1", cc.getMappedPort(cc.getHttpPort()));
        
        Testcontainers.exposeHostPorts(cc.getMappedPort(cc.getHttpPort()));
        
        //  consul acl policy create  -name read-only -rules key_prefix "" {  policy = "read"}
        ExecResult consulresult = cc.execInContainer("consul", "acl", "policy", "create", "-name", 
        		"read-only", "-rules", "key_prefix \"\" {  policy = \"read\"}", "-token", consulMasterToken,
        		"-http-addr=http://127.0.0.1:8501");
		assertTrue(consulresult.getExitCode() == 0);
        
        Exception actualException = null;
        Response<Boolean> savedWithToken = null;
        
        String testYaml = "db:\n" + 
        		"  port: \"3307\"";
        
        try {
            savedWithToken =  client.setKVBinaryValue("test/spring-boot-example/application.yaml", testYaml.getBytes(),consulMasterToken,null,null);
            System.out.println("kv put value: "+savedWithToken.getValue());
        } catch (Exception e) {
            actualException = e;
        }
        
        // set master consul token for spring cloud temp testing
        //System.setProperty("spring.cloud.consul.config.acl-token",consulMasterToken);
        
        vaultContainer = new VaultContainer<>("vault:1.3.2")
                .withVaultToken("foo")
                .withSecretInVault("secret/test/spring-boot-example", "password=password1");
		vaultContainer.start();
		
		// enable vault consul backend
		ExecResult result = vaultContainer.execInContainer("vault", "secrets", "enable", "consul");
		assertTrue(result.getExitCode() == 0);
		
		// configure consul access
		result = vaultContainer.execInContainer("vault", "write", "consul/config/access", "address=host.testcontainers.internal:"+ cc.getMappedPort(cc.getHttpPort()), "token="+consulMasterToken);
		assertTrue(result.getExitCode() == 0);
		
		result = vaultContainer.execInContainer("vault", "write", "consul/roles/consul-read-only", "policies=read-only");
		assertTrue(result.getExitCode() == 0);
		
		result = vaultContainer.execInContainer("vault", "read", "consul/creds/consul-read-only");
		assertTrue(result.getExitCode() == 0);
		
		int port = vaultContainer.getFirstMappedPort();

		System.setProperty("spring.cloud.vault.host", "127.0.0.1");
		System.setProperty("spring.cloud.vault.port", String.valueOf(port));
		System.setProperty("spring.cloud.vault.scheme", "http");
    }

    @AfterClass
    public static void shutdown() {
    	vaultContainer.stop();
    	cc.stop();
    }
    
    @Autowired
    TestRestTemplate template;
    
    @Autowired Environment env;

    @Test
    public void test() {
    	String res = template.getForObject("/config", String.class);

    	// expect secret token to come back from vault KV
    	Assert.assertTrue(res.contains("password1"));
    	
        // expect consul acl token to be available in the env seeded by vault
        Assert.assertNotNull(env.getProperty("spring.cloud.consul.config.acl-token"));

    	// expect config values to come back from consul KV
        Assert.assertTrue(res.contains("3307"));
        
    }

    
    

}
