/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiladores;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrés
 */
public class Compiladores {
    static int index = -1;//usado para manejar la entrada
    static int acepto=0;//para mostrar mensaje de aceptado
    static Registro token  = new Registro(null, null, -1);//token actual
    
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String traduccion = "";
        FileWriter fwriter = null;
        PrintWriter pw = null;
        File path = null;
        
        JsonLexer.initLexer();
        path = JsonLexer.path;
        
        getToken();
        traduccion = element(new int[]{EOF});
        JsonLexer.closeFile();
        
        if(acepto == 0){
            System.out.println("La fuente no contiene errores ");
            try{
                fwriter = new FileWriter(path.getParent()+"\\out.xml");
                pw = new PrintWriter(fwriter);
                pw.print("<?xml version = \"1.0\"?>\n"+traduccion);
            }catch(IOException ex){
                Logger.getLogger(Compiladores.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                try {
                    if(fwriter != null)
                        fwriter.close();
                } 
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    static void getToken(){
        do{
            try{
                JsonLexer.sigLex();
                token = JsonLexer.token;
            }catch(IOException ex){
                Logger.getLogger(Compiladores.class.getName()).log(Level.SEVERE, null, ex);
            }
        }while(token.id == ERROR);
    }
    
    static void sendError(){
        System.out.println("Error de Sintaxis en linea "+ JsonLexer.nroLinea+" no se esperaba "+ token);
        acepto = -1;
        if(token.id == EOF){
            System.exit(0);
        }
    }
    
    static void match(int tokenEs){
        if(token.id == tokenEs ){
            getToken();
        }else{
            sendError();
        }
    }
    
    static void checkinput(int [] firsts , int[] follows ){
        if(!(in(firsts))){
            sendError();
            scanto(union(firsts,follows));
        }
    }
    
    static int[] union(int[] array1, int[] array2){
        int[] array3 = new int[array1.length+array2.length];
        int i = 0;
        for (int s : array1) {
            array3[i] = s;
            i++;
        }
        for (int s : array2) {
            array3[i] = s;
            i++;
        }
        return array3;
    }
    
    static void scanto (int[] synchset){ 
        int consumidos = 0;
        while(!(in(synchset) || token.id==EOF)){
            getToken();
            consumidos++;
        }
        System.out.println("se comsumieron "+consumidos+" tokens");
    }
    
    static boolean in (int[] array){
        for(int s: array){
            if(token.id == s){
                return true;
            }
        }
        return false;
    }
    
    static String sinComillas(String c){
        c = c.substring(1, c.length()-1);
        return c;
    }
    
    static String element(int[] synchset){
        String tagNameT ;
        String auxT;
        checkinput(new int[]{L_CORCHETE,LITERAL_CADENA}, synchset);
        if(!(in(synchset))){
            switch(token.id){
                case L_CORCHETE:
                    match(L_CORCHETE);
                    tagNameT = tagname(new int[]{R_CORCHETE, COMA});
                    auxT = aux(new int[]{R_CORCHETE});
                    match(R_CORCHETE);
                    checkinput(synchset, new int[]{L_CORCHETE,LITERAL_CADENA});//luego de return ya no funciona el ultimo check
                    return "<"+tagNameT+auxT+" </"+tagNameT+">";//element-->[tagname aux]
                case LITERAL_CADENA:
                    String cadena = token.lexema;//despues del match la cadena se pierde por getToken
                    match(LITERAL_CADENA);
                    checkinput(synchset, new int[]{L_CORCHETE,LITERAL_CADENA});
                    return sinComillas(cadena);//element--> string
                default:
                    sendError();
            }
            checkinput(synchset, new int[]{L_CORCHETE,LITERAL_CADENA});
        }
        return "";//aqui se llega solo si ocurre un error de sintaxis
    }
    
    
     static String tagname(int[] synchset) {
        String cadena="";
        checkinput(new int[]{LITERAL_CADENA}, synchset);
        if(!(in(synchset))){
            cadena = token.lexema;//despues del match la cadena se pierde por getToken
            match(LITERAL_CADENA);
        }
        checkinput(synchset, new int[]{LITERAL_CADENA});
        return sinComillas(cadena);//tagname--> string
    }
     
     
    static String aux(int[] synchset) {
        //un caso especial son las funciones que pueden tomar vacio:
        //es valido que venga la coma o que venga algo de su conjunto siguiente
        String aux2Trad = "";
        checkinput(union(new int[]{COMA},synchset), new int[]{});
        if(!(in(synchset))){
            match(COMA);
            aux2Trad = aux2(new int[]{R_CORCHETE});
            checkinput(synchset, new int[]{LITERAL_CADENA});
            return aux2Trad;//aux--> ,aux2
        }
        return " >";//aux--> vacio
    }
    
    static String aux2(int[] synchset) {
        String atributesTrad = "";
        String aux3Trad = "";
        String elementlistTrad = "";
        checkinput(new int[]{L_LLAVE, L_CORCHETE, LITERAL_CADENA }, synchset);
        if(!in(synchset)){
            switch(token.id){
                case L_LLAVE:
                    atributesTrad = atributes(new int[]{COMA, R_CORCHETE});
                    aux3Trad = aux3(new int[]{R_CORCHETE});
                    checkinput(synchset, new int[]{L_LLAVE, L_CORCHETE, LITERAL_CADENA });
                    return " "+atributesTrad+">"+aux3Trad;//aux2--> atributes aux3
                case L_CORCHETE:
                    elementlistTrad = elementlist(new int[]{R_CORCHETE});
                    checkinput(synchset, new int[]{L_LLAVE, L_CORCHETE, LITERAL_CADENA });
                    return ">\n"+elementlistTrad;//aux2--> elementlist
                case LITERAL_CADENA:
                    elementlistTrad = elementlist(new int[]{R_CORCHETE});//el corchete del element que le contiene
                    checkinput(synchset, new int[]{L_LLAVE, L_CORCHETE, LITERAL_CADENA });
                    return "\n"+elementlistTrad;//aux2--> elementlist
                default:
                    sendError();
            }
        }
        checkinput(synchset, new int[]{L_LLAVE, L_CORCHETE, LITERAL_CADENA });
        return "";//aqui se llega solo en caso de error de sintaxis
    }
    
    static String aux3(int[]synchset) {
        String elementlistTrad = "";
        checkinput(union(new int[]{COMA},synchset), synchset);
        if(!(in(synchset))){
            match(COMA);
            elementlistTrad = elementlist(new int[]{R_CORCHETE});
            checkinput(synchset, new int[]{COMA});
            return "\n"+elementlistTrad;//aux3--> ,elementlist
        }
        return "";//aux3--> vacio
    }
    
    static String atributes(int[]synchset) {
        String aux7Trad = "";
        checkinput(new int[]{L_LLAVE}, synchset);
        if(!in(synchset)){
            switch(token.id){
                case L_LLAVE:
                    match(L_LLAVE);
                    aux7Trad = aux7(new int[]{R_LLAVE});
                    match(R_LLAVE);
                    break;
                default:
                    sendError();
            }
        }
        checkinput(synchset, new int[]{L_LLAVE});
        return aux7Trad;//atributes--> {aux7}
    }
    
    static String elementlist(int[] synchset) {
        String elementTrad = "";
        String aux5Trad = "";
        checkinput(new int[]{L_CORCHETE,LITERAL_CADENA}, synchset);
        if(!(in(synchset))){
            switch(token.id){
                case L_CORCHETE:
                    elementTrad = element(new int[]{COMA, R_CORCHETE});
                    aux5Trad = aux5(new int[]{R_CORCHETE});
                    checkinput(synchset, new int[]{L_CORCHETE,LITERAL_CADENA});
                    return elementTrad+aux5Trad;//elementlist--> element aux5
                case LITERAL_CADENA:
                    elementTrad = element(new int[]{COMA, R_CORCHETE});
                    aux5Trad = aux5(new int[]{R_CORCHETE});
                    checkinput(synchset, new int[]{L_CORCHETE,LITERAL_CADENA});
                    return elementTrad+aux5Trad;//elementlist--> element aux5
                default:
                    sendError();
            }
            checkinput(synchset, new int[]{L_CORCHETE,LITERAL_CADENA});
        }
        return "";//aqui se llega solo si ocurre un error de sintaxis
    }
    
    private static String aux7(int[] synchset) {
        String atributeslistTrad = "";
        checkinput(union(new int[]{LITERAL_CADENA},synchset), synchset);
        if(!(in(synchset))){
            atributeslistTrad = atributeslist(new int[]{R_LLAVE});
            checkinput(synchset, new int[]{LITERAL_CADENA});
            return atributeslistTrad;//aux7--> atributeslist
        }
        return "";//aux7--> vacio
    }
    
    private static String atributeslist(int[] synchset) {
        String atributeTrad = "";
        String aux4Trad = "";
        checkinput(new int[]{LITERAL_CADENA}, synchset);
        if(!(in(synchset))){
            switch(token.id){
                case LITERAL_CADENA:
                    atributeTrad = atribute(new int[]{COMA,R_LLAVE});
                    aux4Trad = aux4(new int[]{R_LLAVE});
                    checkinput(synchset, new int[]{LITERAL_CADENA});
                    return atributeTrad+aux4Trad;//atributelist-->atribute aux4  
                default:
                    sendError();
            }
            checkinput(synchset, new int[]{LITERAL_CADENA});
        }
        return "";//aqui se llega solo si ocurre un error de sintaxis
    }
    
    private static String aux5(int[] synchset) {
        String elementTrad = "";
        String aux5Trad = "";
        checkinput(union(new int[]{COMA},synchset), synchset);
        if(!(in(synchset))){
            match(COMA);
            elementTrad = element(new int[]{COMA, R_CORCHETE});
            aux5Trad = aux5(new int[]{R_CORCHETE});
            checkinput(synchset, new int[]{COMA});
            return "\n"+elementTrad+aux5Trad;//aux5--> ,element aux5
        }
        return "";//aux5--> vacio
    }
    
    private static String atribute(int[] synchset) {
        String attribute_nameTrad = "";
        String attribute_valueTrad = "";
        checkinput(new int[]{LITERAL_CADENA}, synchset);
        if(!(in(synchset))){
            switch(token.id){
                case LITERAL_CADENA:
                    attribute_nameTrad = attribute_name(new int[]{DOS_PUNTOS});
                    match(DOS_PUNTOS);
                    attribute_valueTrad = attribute_value(new int[]{COMA,R_LLAVE});
                    checkinput(synchset, new int[]{LITERAL_CADENA});
                    return attribute_nameTrad+" = "+attribute_valueTrad;//atribute--> attribute_name : attribuete_value
                default:
                    sendError();
            }
            checkinput(synchset, new int[]{LITERAL_CADENA});
        }
        return "";//aqui se llega solo si ocurre un error de sintaxis
    }
    
    private static String aux4(int[] synchset) {
        String atributeTrad = "";
        String aux4Trad = "";
        checkinput(union(new int[]{COMA},synchset), new int[]{});
        if(!(in(synchset))){
            match(COMA);
            atributeTrad = atribute(new int[]{COMA,R_LLAVE});
            aux4Trad = aux4(new int[]{R_LLAVE});
            checkinput(synchset, new int[]{COMA});
            return " "+atributeTrad+aux4Trad;//aux4--> , atribute aux4
        }
        return "";//aux4--> vacio
    }
    
    private static String attribute_name(int[] synchset) {
        String cadena = "";
        checkinput(new int[]{LITERAL_CADENA}, synchset);
        if(!(in(synchset))){
            switch(token.id){
                case LITERAL_CADENA:
                    cadena = token.lexema;//despues del match la cadena se pierde por getToken
                    match(LITERAL_CADENA);
                    checkinput(synchset, new int[]{LITERAL_CADENA});
                    return sinComillas(cadena);    
                default:
                    sendError();
            }
            checkinput(synchset, new int[]{LITERAL_CADENA});
        }
        return "";//aqui se llega solo si ocurre un error de sintaxis
    }
    
    private static String attribute_value(int[] synchset) {
        String cadena = "";
        checkinput(new int[]{LITERAL_CADENA,LITERAL_NUM,PR_TRUE,PR_FALSE,PR_NULL}, synchset);
        if(!(in(synchset))){
            switch(token.id){
                case LITERAL_NUM:
                    cadena = token.lexema;
                    match(LITERAL_NUM);
                    checkinput(synchset, new int[]{LITERAL_CADENA,LITERAL_NUM,PR_TRUE,PR_FALSE,PR_NULL});
                    return cadena;//attribute_value--> LITERAL_NUM
                case LITERAL_CADENA:
                    cadena = token.lexema;
                    match(LITERAL_CADENA);
                    checkinput(synchset, new int[]{LITERAL_CADENA,LITERAL_NUM,PR_TRUE,PR_FALSE,PR_NULL});
                    return cadena;//attribute_value--> LITERAL_CADENA
                case PR_TRUE:
                    match(PR_TRUE);
                    checkinput(synchset, new int[]{LITERAL_CADENA,LITERAL_NUM,PR_TRUE,PR_FALSE,PR_NULL});
                    return "true";//attribute_value--> PR_TRUE
                case PR_FALSE:
                    match(PR_FALSE);
                    checkinput(synchset, new int[]{LITERAL_CADENA,LITERAL_NUM,PR_TRUE,PR_FALSE,PR_NULL});
                    return "false";//attribute_value--> PR_FALSE
                case PR_NULL:
                    match(PR_NULL);
                    checkinput(synchset, new int[]{LITERAL_CADENA,LITERAL_NUM,PR_TRUE,PR_FALSE,PR_NULL});
                    return " ";//attribute_value--> PR_NULL
                default:
                    sendError();
            }
            checkinput(synchset, new int[]{LITERAL_CADENA,LITERAL_NUM,PR_TRUE,PR_FALSE,PR_NULL});
        }
        return "";//aqui se llega solo si ocurre un error de sintaxis
    }
}
