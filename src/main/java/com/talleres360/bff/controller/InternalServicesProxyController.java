package com.talleres360.bff.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Reenvia a ms-catalog (8082) y ms-report (8083), cada uno en su propia EC2.
 * Esos micros exigen la cabecera X-Internal-Key: el navegador nunca la conoce, la agrega el BFF.
 */
@RestController
public class InternalServicesProxyController {

	private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

	private final RestClient client;
	private final String catalogUrl;
	private final String reportUrl;
	private final String internalKey;

	public InternalServicesProxyController(RestClient ordersClient,
			@Value("${app.services.catalog-url}") String catalogUrl,
			@Value("${app.services.report-url}") String reportUrl,
			@Value("${app.services.internal-key}") String internalKey) {
		this.client = ordersClient;
		this.catalogUrl = catalogUrl;
		this.reportUrl = reportUrl;
		this.internalKey = internalKey;
	}

	@RequestMapping(
			path = { "/api/products", "/api/products/**" },
			method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT })
	public ResponseEntity<byte[]> products(HttpServletRequest request, @RequestBody(required = false) byte[] body)
			throws IOException {
		return forward(catalogUrl, request, body);
	}

	@RequestMapping(path = "/api/reports/**", method = RequestMethod.GET)
	public ResponseEntity<byte[]> reports(HttpServletRequest request) throws IOException {
		return forward(reportUrl, request, null);
	}

	private ResponseEntity<byte[]> forward(String baseUrl, HttpServletRequest request, byte[] body) {
		URI target = URI.create(baseUrl + request.getRequestURI()
				+ (request.getQueryString() != null ? "?" + request.getQueryString() : ""));

		RestClient.RequestBodySpec spec = client
				.method(HttpMethod.valueOf(request.getMethod()))
				.uri(target)
				.header(INTERNAL_KEY_HEADER, internalKey);

		Optional.ofNullable(request.getContentType())
				.ifPresent(contentType -> spec.contentType(MediaType.parseMediaType(contentType)));
		if (body != null && body.length > 0) {
			spec.body(body);
		}

		// Devuelve estado y cuerpo tal cual (incluidos 400/401/404/409 del micro)
		return spec.exchange((req, res) -> {
			HttpHeaders headers = new HttpHeaders();
			Optional.ofNullable(res.getHeaders().getContentType()).ifPresent(headers::setContentType);
			return ResponseEntity.status(res.getStatusCode())
					.headers(headers)
					.body(res.getBody().readAllBytes());
		}, false);
	}
}
