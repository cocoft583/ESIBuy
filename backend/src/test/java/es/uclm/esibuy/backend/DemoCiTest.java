package es.uclm.esibuy.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DemoCiTest {

    @Test
    void falloProvocadoParaLaDemo() {
        assertEquals(4, 2 + 3, "Fallo provocado a propósito para la demo de CI");
    }
}
