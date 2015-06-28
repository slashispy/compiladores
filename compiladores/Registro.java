/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiladores;

/**
 *
 * @author Andrés
 */
public class Registro {
    String comLex;
    String lexema;
    int id;

    public Registro(String comLex, String lexema, int id) {
        this.comLex = comLex;
        this.lexema = lexema;
        this.id = id;
    }

    @Override
    public String toString() {
        return "" + comLex + "  ";
    }
}
