package ai.factory.agent_fitness.chat;

import ai.factory.agent_fitness.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class StaticResourceTests {
    @Autowired
    MockMvc mvc;

    @Test
    void optionalBrowserResourcesReturnNotFoundInsteadOfServiceUnavailable() throws Exception {
        for (String path : java.util.List.of("/.well-known/appspecific/com.chrome.devtools.json", "/missing-file.js")) {
            mvc.perform(get(path)).andExpect(status().isNotFound())
                    .andExpect(content().json("{\"code\":\"NOT_FOUND\"}"));
        }
    }

    @Test
    void pageReferencesAnAvailableFavicon() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/favicon.svg\"")));
        mvc.perform(get("/favicon.svg")).andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));
    }
}
