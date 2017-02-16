package gov.nist.hit.gvt.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@WebFilter(urlPatterns = "*")
@Order(Ordered.LOWEST_PRECEDENCE)
public class GvtCorsFilter implements Filter {

	/*
	 * flaw: Browser Mime Sniffing - fix: X-Content-Type-Options flaw: Cached
	 * SSL Content - fix: Cache-Control flaw: Cross-Frame Scripting - fix:
	 * X-Frame-Options flaw: Cross-Site Scripting - fix: X-XSS-Protection flaw:
	 * Force SSL - fix: Strict-Transport-Security
	 * 
	 * assure no-cache for login page to prevent IE from caching
	 */

	protected final Log logger = LogFactory.getLog(getClass());

	private FilterConfig filterConfig;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	@Override
	public void destroy() {
		this.filterConfig = null;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (response instanceof HttpServletResponse) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
			httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			httpResponse.setHeader("X-Content-Type-Options", "nosniff");
			httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
			httpResponse.setHeader("Access-Control-Allow-Origin", "*");
			httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
		    httpResponse.setHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
		    httpResponse.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers,Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization,appVersion,rsbVersion");

			// httpResponse.setHeader("Access-Control-Expose-Headers", "dTime");
		}

		chain.doFilter(request, response);

	}

}
