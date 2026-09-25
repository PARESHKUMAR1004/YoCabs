package com.yocabs.api;

import com.yocabs.api.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class ApiApplicationTests extends IntegrationTestBase {

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		org.junit.jupiter.api.Assertions.assertEquals(200, status(get("/actuator/health", null)));
	}

}
