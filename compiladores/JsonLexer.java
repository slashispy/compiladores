/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiladores;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Character.isDigit;
import java.util.LinkedList;
import java.util.Scanner;

/**
 *
 * @author Andrés
 */
public class JsonLexer {
    public static BufferedReaderExtendido archivo;
    public static int nroLinea = 1;
    public static String literal ="";
    public static Registro token = null;
    public static LinkedList<Registro> lista =  new LinkedList<Registro>();
    public static Registro[] reservedWord = new Registro[3];//solo hay 3 palabras reservadas
    public static FileWriter fwriter = null;
    public static PrintWriter pw = null;
    public static FileReader freader = null;
    public static File path = null;
    
    
    /*Conjunto de Caracteres Disponibles Terminales */
    public static final int L_CORCHETE = 1;
    public static final int R_CORCHETE = 2;
    public static final int L_LLAVE = 3;
    public static final int R_LLAVE = 4; 
    public static final int COMA = 5;
    public static final int DOS_PUNTOS = 6; 
    public static final int LITERAL_CADENA = 7;
    public static final int LITERAL_NUM = 8;
    public static final int PR_TRUE = 9;
    public static final int PR_FALSE = 10;
    public static final int PR_NULL = 11;
    public static final int EOF = 12;
    public static final int ERROR = -1;
    
    
    public static void main(String[] args) {
        initReservedWord();
        Scanner ingreso = new Scanner(System.in);
        do {
            System.out.println("Ingrese La ruta del archivo:");
            path = new File (ingreso.nextLine());
            
        }while(!path.canRead());
        
        try {
            freader = new FileReader (path);
            archivo = new BufferedReaderExtendido(freader);
            do{
                sigLex(); //Siguiente Lexema 
                
            }while(!("EOF".equals(lista.get(lista.size()-1).comLex)));
            fwriter = new FileWriter (path.getParent()+"/output.txt");
            pw = new PrintWriter(fwriter);
            for (Registro r : lista){
                pw.print(r.comLex+"  ");
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try {
                if(fwriter != null)
                    fwriter.close();
                if(freader != null)
                    freader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } 
    }
 
    public static void initReservedWord(){
        reservedWord[0] = new Registro("PR_TRUE", "TRUE", 9);
        reservedWord[1] = new Registro("PR_FALSE", "FALSE", 10);
        reservedWord[2] = new Registro("PR_NULL", "NULL", 11);
    }
      
    public static LinkedList initLexer(){
        Scanner ingreso = new Scanner(System.in);
        FileReader freader = null;
        
        do{
            System.out.println("Ingrese La ruta del archivo: ");
            path = new File(ingreso.nextLine());
        }while(!path.canRead());
        try{
            freader = new FileReader (path);
            archivo = new BufferedReaderExtendido(freader);
        }catch(Exception e){
            e.printStackTrace();
        }
        return lista;
    }
    
    public static void closeFile(){
        try{
            if(fwriter != null){
                fwriter.close();
            }
            if(freader != null){
                freader.close();
            }
            if(archivo != null){
                archivo.close();
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    public static void sendError(String msg){
        System.out.println(String.format("Linea %-4d"+" "+msg,nroLinea));
        token = new Registro("ERROR","ERROR", -1);
    }


    public static void sigLex() throws IOException{
        char caracter;
        while ((caracter = archivo.getchar()) != (char)-1){
            literal = "";
            if(caracter == ' '|| caracter == '\t' || caracter == '\r'){
                continue;
            }else 
            if(caracter == '\n'){
                nroLinea++;
                continue;
            }else if(caracter == '"'){//empieza reconocimiento String
                int estado = 1 ;
                boolean acepto = false;
                literal = literal + caracter;
                while(!acepto){
                    switch(estado){
                        case(1):
                            caracter = archivo.getchar();
                            if(caracter == '\\'){
                                literal = literal + caracter;
                                estado = 2;
                            }else if(caracter == '"'){
                                literal = literal + caracter;
                                estado = 3;
                            }else if(caracter == '\n'){
                                estado = -1;
                            }else if(caracter == (char)-1){
                                estado = -1;
                            }else{
                                literal = literal + caracter ;
                                estado = 1;
                            }
                            break;
                        case(2):
                            caracter = archivo.getchar();
                            if(caracter == '"'){
                                literal = literal + caracter;
                                estado = 1;
                            }else if(caracter == 'n'){
                                literal = literal + caracter;
                                estado = 1;
                            }else if(caracter == 't'){
                                literal += caracter;
                                estado = 1;
                            }else if (caracter == 'f'){
                                literal += caracter;
                                estado = 1;
                            }else if (caracter == 'b'){
                                literal += caracter;
                                estado = 1;
                            }else if (caracter == 'r'){
                                literal += caracter;
                                estado = 1;
                            }else if (caracter == '\\'){
                                literal += caracter ;
                                estado = 1;
                            }else if (caracter == '/'){
                                literal += caracter;
                                estado = 1;
                            }else if (caracter == 'u'){
                                literal += caracter;
                                estado = 1;
                            }else
                                estado = -2;
                                break;
                        case(3):
                            acepto = true;
                            lista.add(token = new Registro("LITERAL_CADENA", literal, LITERAL_CADENA));
                            break;
                        case(-1):
                            if(caracter == '\n'){
                                archivo.ungetchar(); //devuelve el caracter para que no haya perdida de lexemas
                            }
                            sendError("Literal Cadena Incorrecto");
                            return;
                        case(-2):
                            sendError("Caracter de Escape incorrecto encontrado");
                            char c = caracter;
                            while(c!= '\n' && c!= (char)-1){
                                c = archivo.getchar();
                                
                            }
                            archivo.ungetchar();
                            return;
                    }
                }
                break;
            }else if (caracter == ':'){
                lista.add(token = new Registro ("DOS_PUNTOS", ":", DOS_PUNTOS));
                break;
            }else if (caracter == '['){
                lista.add(token = new Registro ("L_CORCHETE", "[", L_CORCHETE));
                break;
            }else if (caracter == ']'){
                lista.add(token = new Registro ("R_CORCHETE", "]", R_CORCHETE));
                break;
            }else if (caracter == '{'){
                lista.add(token = new Registro ("L_LLAVE", "{", L_LLAVE));
                break;
            }else if (caracter == '}'){
                lista.add(token = new Registro("R_LLAVE", "}", R_LLAVE));
                break;
            }else if (caracter == ','){
                lista.add(token = new Registro("COMA", ",", COMA));
                break;
            }else if(Character.isLetter(caracter)){
                do{
                    literal += caracter;
                    caracter = archivo.getchar();
                }while(Character.isLetter(caracter));
                archivo.ungetchar();
                for (Registro word : reservedWord){
                if(word.lexema.equalsIgnoreCase(literal)){
                    lista.add(token = new Registro(word.lexema, literal, word.id));
                    return;
                }
            }
            sendError("Lexema no Valido "+ literal);
            return;
            }else if(isDigit(caracter)){ //consulta si es un numero
                int i = 0;
                int estado = 0;
                boolean acepto = false ;
                literal += caracter;
                while(!acepto){
                    switch(estado){
                        case (0): //una secuencia netamente de digitos, puede ocurrir . o e
                            caracter = archivo.getchar();
                            if(isDigit(caracter)){
                                literal += caracter;
                                estado = 0;
                            }else if(caracter == '.'){
                                literal += caracter;
                                estado = 1;
                            }else if(Character.toLowerCase(caracter)== 'e'){
                                literal += caracter;
                                estado = 3;
                            }else{
                                estado = 6;
                            }
                            break;
                        case (1): //punto, debe seguir numero
                            caracter = archivo.getchar();
                            if(isDigit(caracter)){
                                literal += caracter;
                                estado = 2;
                            } else {
                                sendError("no se esperaba "+ literal);
                                estado = -1;
                            }
                            break;
                        case (2): //la fraccion decimal, pueden seguir digitos o e
                            caracter = archivo.getchar();
                            if(isDigit(caracter)){
                                literal += caracter;
                                estado = 2;
                            }else if(Character.toLowerCase(caracter)=='e'){
                                literal += caracter;
                                estado = 3;
                            }else{
                                estado = 6;
                            }
                            break;
                        case (3): //una e, puede seguir +, - o secuencia de digitos
                            caracter = archivo.getchar();
                            if(caracter == '+' || caracter == '-'){
                                literal += caracter;
                                estado = 4;
                            }else if(isDigit(caracter)){
                                literal += caracter;
                                estado = 5;
                            }else{
                                sendError("no se esperaba "+ literal);
                                estado = -1;
                            }
                            break;
                        case (4): //necesariamente debe venir un digito al menos
                            caracter = archivo.getchar();
                            if(isDigit(caracter)){
                                literal += caracter;
                                estado=5;
                            }else{
                                sendError("no se esperaba "+literal);
                                estado = -1;
                            }
                            break;
                        case (5)://exponente
                            caracter = archivo.getchar();
                            if(isDigit(caracter)){
                                literal += caracter;
                                estado = 5;
                            }else{
                                estado = 6;
                            }
                            break;
                        case (6)://estado de aceptacion
                            if (caracter != (char)-1){
                                archivo.ungetchar();
                            }else{
                                caracter = (char)0;
                            }
                            acepto = true;
                            lista.add(token = new Registro ("LITERAL_NUM", literal, LITERAL_NUM));
                            break;
                        case (-1):
                            if (caracter != (char)-1){
                                sendError("No se esperaba "+ literal);
                            }else if(caracter == '\n'){
                                archivo.ungetchar();
                                sendError("No se esperaba fin de Linea");
                                
                            }
                        return;
                    }
                }
            }else{
                sendError("caracter no valido");
            }
        }if(caracter == (char)-1){
            lista.add(token = new Registro("EOF", "eof", EOF));
        }
    }
    
}