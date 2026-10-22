package rsnrdk.websec.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rsnrdk.websec.controller.ItemController;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.service.ItemService;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreatedPostMappingTest {

    private static final String BODY = "{\"name\":\"Laptop\"}";

    private MockMvc mockMvc;

    /**
     * Reference controller that uses the ORIGINAL two annotations separately.
     */
    @RestController
    @RequestMapping("/reference/items")
    static class ReferenceController {

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public ItemDto create(@RequestBody ItemDto dto) {
            dto.setId(1L);
            return dto;
        }
    }

    @BeforeEach
    void setUp() {
        ItemService itemService = mock(ItemService.class);
        when(itemService.createItem(any(ItemDto.class)))
                .thenReturn(ItemDto.builder().id(1L).name("Laptop").build());

        // no Spring context: controllers are registered directly in a standalone MockMvc
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ItemController(itemService), new ReferenceController())
                .build();
    }

    @Test
    void post_composedAnnotation_returns201AndRoutesToHandler() throws Exception {
        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void post_originalAnnotations_returnSame201() throws Exception {
        mockMvc.perform(post("/reference/items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void patch_onComposedEndpointPath_returnsMethodNotAllowed() throws Exception {
        // the composed annotation maps POST only; GET still reaches the separate @GetMapping
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void mergedAttributes_composedAndOriginal_areIdentical() throws Exception {
        // Arrange
        Method composed = ItemController.class.getMethod("createItem", ItemDto.class);
        Method original = ReferenceController.class.getMethod("create", ItemDto.class);

        // Act: this is how Spring itself reads annotations, including meta-annotations
        RequestMapping composedMapping = AnnotatedElementUtils.findMergedAnnotation(composed, RequestMapping.class);
        RequestMapping originalMapping = AnnotatedElementUtils.findMergedAnnotation(original, RequestMapping.class);
        ResponseStatus composedStatus = AnnotatedElementUtils.findMergedAnnotation(composed, ResponseStatus.class);
        ResponseStatus originalStatus = AnnotatedElementUtils.findMergedAnnotation(original, ResponseStatus.class);

        // Assert
        assertArrayEquals(new RequestMethod[]{RequestMethod.POST}, composedMapping.method());
        assertArrayEquals(originalMapping.method(), composedMapping.method());
        assertEquals(HttpStatus.CREATED, composedStatus.code());
        assertEquals(originalStatus.code(), composedStatus.code());
    }
}