package com.example.cashcard;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/**
 * Con esta anotación spring arranca un servidor web embebido en un puerto
 * aleatorio disponible
 */
class CashcardApplicationTests {

	@Autowired
	// Clase que nos ayuda a simular un cliente
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		/** version sin autenticación */
		/*
		 * ResponseEntity<String> response = restTemplate.getForEntity("/cashcards/99",
		 * String.class);
		 * assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		 */

		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").
			getForEntity("/cashcards/99", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		/*
		 * ResponseEntity<String> response =
		 * restTemplate.getForEntity("/cashcards/1000", String.class);
		 * assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		 */
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards/1000",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	/**
	 * DirtiesContext devuelve establece o hace que no se modifique el estado
	 * inicial
	 * en el que se ejecutan los tests, ya que pueden interferir entre ellos, por
	 * ejemplo este
	 * test introduce un nuevo valor en la vase de datos, lo que haría que el test
	 * shouldReturnAllCashCardsWhenListIsRequested() fallase
	 */
	@Test
	@DirtiesContext
	void shouldCreateANewCashCard() {
		CashCard newCashCard = new CashCard(null, 250.00, null);
		// En este caso no esperamos como respuesta un body como en el GET, por eso
		// devuelve un Void response body
		/*ResponseEntity<Void> createResponse = restTemplate.postForEntity("/cashcards", newCashCard, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);*/
		ResponseEntity<Void> createResponse = restTemplate.withBasicAuth("sarah1", "abc123").
			postForEntity("/cashcards",newCashCard, Void.class);
			assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity(locationOfNewCashCard, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);
	}

	/** Pagination test */
	@Test
	void shouldReturnAPageOfCashCards() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards?page=0&size=1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	/** Sorting test */
	@Test
	void shouldReturnASortedPageOfCashCards() {
		/**
		 * page: es el numero de paginas, empiezan en 0(0-indexed)
		 * size: numero de elementos por pagina
		 * sort: campo de ordenamiento(asc o desc)
		 */
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards?page=0&size=1&sort=amount,desc",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");
		assertThat(read.size()).isEqualTo(1);
		

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);

	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials(){
		ResponseEntity<String> response = restTemplate.
		withBasicAuth("bad", "abc123").
		getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate.
		withBasicAuth("sarah1", "bad").
		getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectUsersWhoAreNotCardOwners(){
		ResponseEntity<String> response = restTemplate.withBasicAuth("hank-owns-no-cards", "qrs456")
		.getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn(){
		ResponseEntity<String> response = restTemplate
			.withBasicAuth("sarah1", "abc123")
			.getForEntity("/cashcards/102", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard() {
		/**
		 * No hay implementado un putForEntity, en su lugar usamos un metodo más
		 * generalizado y explicito(exchange)
		 * que requiere que el verbo y el request entity(body entity) sean pasado como
		 * parametros.
		 */
		CashCard cashCardUpdate = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
		ResponseEntity<Void> response = restTemplate.withBasicAuth("sarah1", "abc123")
		.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);

	}

	@Test
	void shouldNotUpadeACashcardThatDoesNotExist(){
		CashCard unknownCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
		ResponseEntity<Void> response= restTemplate
			.withBasicAuth("sarah1", "abc123")
			.exchange("/cashcards/999", HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpadteACashCardThatIsOwnedBySomeoneElse(){
		CashCard kumarCard = new CashCard(null, 333.33, null);
		HttpEntity<CashCard> request = new HttpEntity<CashCard>(kumarCard);
		ResponseEntity<Void> response = restTemplate
			.withBasicAuth("sarah1", "abc123")
			.exchange("/cashcards/102", HttpMethod.PUT, request, Void.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	@Test
	@DirtiesContext
	void shouldDeleteAnExistingCashCard(){
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		//Aqui testeamos el intento de borrar una CashCard que acabamos de borrar
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteACashCardThatDoesNotExist(){
		ResponseEntity<Void> response = restTemplate
			.withBasicAuth("sarah1", "abc123")
			.exchange("/cashcards/999", HttpMethod.DELETE, null, Void.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn(){
		ResponseEntity<Void> response = restTemplate
			.withBasicAuth("sarah1", "abc123")
			.exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		//Comprobamos que el registro que hemos intentado borrar aún existe
		ResponseEntity<String> getResponse = restTemplate
            .withBasicAuth("kumar2", "xyz789")
            .getForEntity("/cashcards/102", String.class);
    	assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

	}
	
}
