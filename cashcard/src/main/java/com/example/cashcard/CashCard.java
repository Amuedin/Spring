package com.example.cashcard;

import org.springframework.data.annotation.Id;

/**
 * Con el modificador record designamos la clase como inmutable, y que solo sirve para contener datos
 * Con el encabezado(parametros) el compilador genera automaticamente los campos como privados, el modificador final, un constructor 
 * y los getters de los campos sin el prefijo get.
 * Este sería un ejemplo de objeto de dominio de la aplicación
 * Con @Id Spring Data ya reconoce que campo será tomado como id del objeto
 */
record CashCard(@Id Long id, Double amount, String owner) {
    
}
