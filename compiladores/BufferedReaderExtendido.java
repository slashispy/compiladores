/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiladores;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author Andrés
 */
public class BufferedReaderExtendido extends BufferedReader {
     public BufferedReaderExtendido(Reader in) {
        super(in);
    }
    
     public char getchar() throws IOException{
        mark(1);
        return  (char)this.read();
    }
    
    public void ungetchar() throws IOException{
        reset();
    }
}
