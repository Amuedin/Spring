package com.example.cashcard;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;


/**
 * Ejemplo para el uso del enfoque Red Green Refactor
 * Primero haremos que el test falle, despues modificaremos el código mínimo
 * para que pase la prueba
 */
@JsonTest
/**
 * Marca la clase como una clase test, permite usar el framework Jackson
 * incluido en Spring, para el procesamiento y
 * testeo de JSON
 */
public class CashCardJsonTest {
    /*
     * V1
     * 
     * @Test
     * void myFirstTest(){
     * assertThat(42).isEqualTo(42);
     * }
     */

    @Autowired
    private JacksonTester<CashCard> json;

    @Autowired
    private JacksonTester<CashCard[]> jsonList;

    private CashCard[] cashCards;

    @Autowired
    private static final Logger log = LoggerFactory.getLogger(CashCardJsonTest.class);

    @BeforeEach
    void setUp() {
        cashCards = Arrays.array(
                new CashCard(99L, 123.45, "sarah1"),
                new CashCard(100L, 1.00, "sarah1"),
                new CashCard(101L, 150.00, "sarah1"));
    }

    @Test
    void mostrarRuta() {
        var resource = getClass().getResource("list.json");
        log.info("Ruta de list.json: " + resource.getPath());
    }

    /**
     * Este test serializa el array en JSON y lo compara con el contenido de
     * list.json
     */
    @Test
    void cashCardListSerializationTest() throws IOException {
        assertThat(jsonList.write(cashCards)).isStrictlyEqualToJson("list.json");

    }

    @Test
    void cashCardListDeserializationTest() throws IOException {
        String expected = """
                       [
                   { "id": 99, "amount": 123.45, "owner": "sarah1" },
                   { "id": 100, "amount": 1.00, "owner": "sarah1" },
                   { "id": 101, "amount": 150.00, "owner": "sarah1" }
                ]
                       """;

        assertThat(jsonList.parse(expected)).isEqualTo(cashCards);
    }

    @Test
    /** Test de serialización */
    void cashCardSerializationTest() throws IOException {
        CashCard cashCard = new CashCard(99L, 123.45, "sarah1");

        // Estos test convierten el objeto java en un documento JSON(serializar) y
        // realiza comprobaciones
        assertThat(json.write(cashCard)).isStrictlyEqualToJson("expected.json");
        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.id");
        assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.id").isEqualTo(99);
        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.amount");
        assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.amount")
                .isEqualTo(123.45);
    }

    @Test
    /** Test de deserialiación */
    void cashCardDeserializationTest() throws IOException {
        String expected = """
                {
                    "id":99,
                    "amount":123.45,
                    "owner": "sarah1"
                }
                """;

        // Ahora al contrario deserializa un documento y lo convierte en un objeto para
        // compararlo con otro objeto
        assertThat(json.parse(expected)).isEqualTo(new CashCard(99L, 123.45, "sarah1"));
        assertThat(json.parseObject(expected).id()).isEqualTo(99);
        assertThat(json.parseObject(expected).amount()).isEqualTo(123.45);
    }

}
