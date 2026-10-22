package rsnrdk.websec.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsrfCookieFilterTest {

    @Mock
    private FilterChain filterChain;

    @Mock
    private CsrfToken csrfToken;

    private final CsrfCookieFilter filter = new CsrfCookieFilter();

    @Test
    void doFilter_tokenUnderClassNameAttribute_loadsTokenSoCookieIsWritten() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(CsrfToken.class.getName(), csrfToken);

        filter.doFilter(request, response, filterChain);

        verify(csrfToken).getToken();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_tokenUnderFallbackAttribute_loadsToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute("_csrf", csrfToken);

        filter.doFilter(request, response, filterChain);

        verify(csrfToken).getToken();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_noToken_stillContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}