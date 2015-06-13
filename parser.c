/******************** LIbrerias *******************/

#include<stdio.h>
#include<string.h>
#include<stdlib.h>
#include<ctype.h>

/***************** Constantes **********************/

#define TAMBUFF 	5
#define TAMLEX 		6
#define TAMHASH 	101

/************* Definiciones ********************/

typedef struct entrada{
	//definir los campos de 1 entrada de la tabla de simbolos
	char *compLex;
	char lexema[TAMLEX];	
	struct entrada *tipoDato; // null puede representar variable no declarada	
	// aqui irian mas atributos para funciones y procedimientos...
	
} entrada;

typedef struct {
	char *compLex;
	entrada *pe;
} token;

/************* Variables globales **************/

int consumir;			/* 1 indica al analizador lexico que debe devolver
						el sgte componente lexico, 0 debe devolver el actual */

char cad[5*TAMLEX];		// string utilizado para cargar mensajes de error
token t;				// token global para recibir componentes del Analizador Lexico

// variables para el analizador lexico

FILE *archivo;			// Fuente Json
FILE *archSalida;		// Archivo de salida
char buff[2*TAMBUFF];	// Buffer para lectura de archivo fuente
char id[TAMLEX];		// Utilizado por el analizador lexico
int delantero=-1;		// Utilizado por el analizador lexico
int fin=0;				// Utilizado por el analizador lexico
int numLinea=1;			// Numero de Linea

/************** Prototipos *********************/
char * attributes_list (char *);
char * attributes_list1 (char *);
void sigLex();	
char * element (char *);
	// Del analizador Lexico

/**************** Funciones **********************/

/*********************HASH************************/
entrada *tabla;				//declarar la tabla de simbolos
int tamTabla=TAMHASH;		//utilizado para cuando se debe hacer rehash
int elems=0;				//utilizado para cuando se debe hacer rehash

int h(const char* k, int m)
{
	unsigned h=0,g;
	int i;
	for (i=0;i<strlen(k);i++)
	{
		h=(h << 4) + k[i];
		if (g=h&0xf0000000){
			h=h^(g>>24);
			h=h^g;
		}
	}
	return h%m;
}
void insertar(entrada e);

void initTabla()
{	
	int i=0;
	
	tabla=(entrada*)malloc(tamTabla*sizeof(entrada));
	for(i=0;i<tamTabla;i++)
	{
		tabla[i].compLex="-1";
	}
}

int esprimo(int n)
{
	int i;
	for(i=3;i*i<=n;i+=2)
		if (n%i==0)
			return 0;
	return 1;
}

int siguiente_primo(int n)
{
	if (n%2==0)
		n++;
	for (;!esprimo(n);n+=2);

	return n;
}

//en caso de que la tabla llegue al limite, duplicar el tamaño
void rehash()
{
	entrada *vieja;
	int i;
	vieja=tabla;
	tamTabla=siguiente_primo(2*tamTabla);
	initTabla();
	for (i=0;i<tamTabla/2;i++)
	{
		if(vieja[i].compLex!="-1")
			insertar(vieja[i]);
	}		
	free(vieja);
}

char * SC (char *str1) {
   //char *str1 = "\"abcdefghi\"";
   int cant = strlen (str1);
   char algo [cant];
   strcpy (algo, str1);
   char aux [cant-2];
   int i;
   for (i = 1; i<cant-1; i++){
       aux [i-1]= algo [i];
   }
   aux [cant-2] = '\0';
   char * aux2 = aux;
   return aux2;
}

//insertar una entrada en la tabla
void insertar(entrada e)
{
	int pos;
	if (++elems>=tamTabla/2)
		rehash();
	pos=h(e.lexema,tamTabla);
	while (tabla[pos].compLex!="-1")
	{
		pos++;
		if (pos==tamTabla)
			pos=0;
	}
	tabla[pos]=e;

}
//busca una clave en la tabla, si no existe devuelve NULL, posicion en caso contrario
entrada* buscar(const char *clave)
{
	int pos;
	entrada *e;
	pos=h(clave,tamTabla);
	while(tabla[pos].compLex!="-1" && strcmp(tabla[pos].lexema,clave)!=0 )
	{
		pos++;
		if (pos==tamTabla)
			pos=0;
	}
	return &tabla[pos];
}

void insertTablaSimbolos(const char *s, char *cl)
{
	entrada e;
	sprintf(e.lexema,s);
	e.compLex=cl;
	insertar(e);
}

void initTablaSimbolos()
{
	insertTablaSimbolos(",","COMA");
	insertTablaSimbolos(".","PUNTO");
	insertTablaSimbolos(":","DOS_PUNTOS");
	insertTablaSimbolos("[","L_CORCHETE");
	insertTablaSimbolos("]","R_CORCHETE");
	insertTablaSimbolos("{","L_LLAVE");
	insertTablaSimbolos("}","R_LLAVE");
	insertTablaSimbolos("true","PR_TRUE");
	insertTablaSimbolos("false","PR_FALSE");
	insertTablaSimbolos("null","PR_NULL");
}

// Funcion para llegar al final de la linea
void linea_sgte()
{   char c;
	c=fgetc(archivo);
	while(c!='\n' && c!=EOF)
	{	c=fgetc(archivo);
	}
	ungetc(c,archivo);
}

void error(const char* mensaje)
{	printf("\nLin %d: Error Lexico. %s.",numLinea,mensaje);
	fprintf(archSalida,"\nLin %d: Error Lexico. %s.",numLinea,mensaje);
	linea_sgte();
}

void error2(const char* mensaje)
{	
	printf("\nLin %d: Error Sintactico. %s.",numLinea,mensaje);
}




void sigLex()
{
	int i=0, longid=0;
	char c=0;
	char linea[500];
	int acepto=0;
	int estado=0;
	char msg[41];
	entrada e;

	while((c=fgetc(archivo))!=EOF)
	{
		
		if (c==' ' || c=='\t')
			continue;	//eliminar espacios en blanco
		else if(c=='\n')
		{
			//incrementar el numero de linea
			numLinea++;
			continue;
		}
		else if (isalpha(c))
		{
			//puede ser true, false, null
			i=0;
			do{
				id[i]=tolower(c);
				i++;
				c=fgetc(archivo);
				if (i>=TAMLEX)
					break;
				
			}while(isalpha(c));
			id[i]='\0';
			if (c!=EOF)
				ungetc(c,archivo);
			else
				c=0;
			t.pe=buscar(id);
			t.compLex=t.pe->compLex;
			if (t.pe->compLex=="-1")
			{	error("Palabra clave incorrecta");
				break;
			}
			break;
		}
		else if (isdigit(c))
		{
				//es un numero
				i=0;
				estado=0;
				acepto=0;
				id[i]=c;
				
				while(!acepto)
				{
					switch(estado){
					case 0: //una secuencia netamente de digitos, puede ocurrir . o e
						c=fgetc(archivo);
						if (isdigit(c))
						{
							id[++i]=c;
							estado=0;
						}
						else if(c=='.'){
							id[++i]=c;
							estado=1;
						}
						else if(tolower(c)=='e'){
							id[++i]=c;
							estado=3;
						}
						else{
							estado=6;
						}
						break;
					
					case 1://un punto, debe seguir un digito 
						c=fgetc(archivo);						
						if (isdigit(c))
						{
							id[++i]=c;
							estado=2;
						}
						else{
							if (c == '\n') 
							{
								ungetc (c, archivo);
								sprintf(msg,"No se esperaba \\n");
								estado=-1;
							}	
							else 
							{	sprintf(msg,"No se esperaba '%c'",c);
								estado=-1;
							}
						}
						break;
					case 2://la fraccion decimal, pueden seguir los digitos o e
						c=fgetc(archivo);
						if (isdigit(c))
						{
							id[++i]=c;
							estado=2;
						}
						else if(tolower(c)=='e')
						{
							id[++i]=c;
							estado=3;
						}
						else
							estado=6;
						break;
					case 3://una e, puede seguir +, - o una secuencia de digitos
						c=fgetc(archivo);
						if (c=='+' || c=='-')
						{
							id[++i]=c;
							estado=4;
						}
						else if(isdigit(c))
						{
							id[++i]=c;
							estado=5;
						}
						else{
							if (c == '\n') 
							{
								ungetc (c, archivo);
								sprintf(msg,"No se esperaba \\n");
								estado=-1;
							}	
							else 
							{	sprintf(msg,"No se esperaba '%c'",c);
								estado=-1;
							}
						}
						break;
					case 4://necesariamente debe venir por lo menos un digito
						c=fgetc(archivo);
						if (isdigit(c))
						{
							id[++i]=c;
							estado=5;
						}
						else{
							if (c == '\n') 
							{
								ungetc (c, archivo);
								sprintf(msg,"No se esperaba \\n");
								estado=-1;
							}	
							else 
							{	sprintf(msg,"No se esperaba '%c'",c);
								estado=-1;
							}
						}
						break;
					case 5://una secuencia de digitos correspondiente al exponente
						c=fgetc(archivo);
						if (isdigit(c))
						{
							id[++i]=c;
							estado=5;
						}
						else{
							estado=6;
						}break;
					case 6://estado de aceptacion, devolver el caracter correspondiente a otro componente lexico
						if (c!=EOF)
							ungetc(c,archivo);
						else
							c=0;
						id[++i]='\0';
						acepto=1;
						t.pe=buscar(id);
						if (t.pe->compLex=="-1")
						{
							sprintf(e.lexema,id);
							e.compLex="LITERAL_NUM";
							insertar(e);
							t.pe=buscar(id);
						}
						t.compLex="LITERAL_NUM";
						break;
					case -1:
						if (c==EOF)
							error("No se esperaba el fin de archivo");
						else
							error(msg);
						acepto=1;
						t.compLex="-1";
						break;
						
					}
				}
			break;
		}
		else if (c==':')
		{
			t.compLex="DOS_PUNTOS";
			t.pe=buscar(":");
			break;
		}
		else if (c==',')
		{
			t.compLex="COMA";
			t.pe=buscar(",");
			break;
		}
		else if (c=='[')
		{
			t.compLex="L_CORCHETE";
			t.pe=buscar("[");
			break;
		}
		else if (c==']')
		{
			t.compLex="R_CORCHETE";
			t.pe=buscar("]");
			break;
		}
		else if (c=='{')
		{
			t.compLex="L_LLAVE";
			t.pe=buscar("{");
			break;
		}
		else if (c=='}')
		{
			t.compLex="R_LLAVE";
			t.pe=buscar("}");
			break;
		}
		else if (c=='\"')
		{//un caracter o una cadena de caracteres
			i=0;
			id[i]=c;
			i++;
			do{
				c=fgetc(archivo);
				if (c=='\"')
				{
					id[i]=c;
					i++;
					break;
				}
				else if(c==EOF)
				{
					error("Se llego al fin de archivo sin finalizar un literal");
				}
				else if(c=='\n')
				{	ungetc(c,archivo);
					error("Se llego al fin de linea sin finalizar un literal"); 
					break;
				}
				else{
					id[i]=c;
					i++;
				}
			}while(isascii(c));
			id[i]='\0';
			if (c==EOF)
				c=0;
			if(id[i-1]!='\"' || i<2)
			{	t.compLex="-1";
				break;
			}
			t.pe=buscar(id);
			t.compLex=t.pe->compLex;
			if (t.pe->compLex=="-1")
			{
				strcpy(e.lexema,id);
				e.compLex="LITERAL_CADENA";
				//printf("\nEntrada: %s",e.lexema);
				insertar(e);
				t.pe=buscar(id);
				t.compLex=e.compLex;
				//printf("\tLexema: %s",t.pe->lexema);
				printf ("\nID: %d",strlen(id));
				printf ("\t %s", id);
				printf ("\nLexema: %d",strlen(t.pe->lexema));
				printf ("\t %s", t.pe->lexema);
				
			}
						
			break;
		}
	}
	if (c==EOF)
	{
		t.compLex="EOF";
		sprintf(e.lexema,"EOF");
		t.pe=&e;
	}
	
}
void scanto (char *synchset)
{	int cant = strlen (synchset);
	char aux [cant+4]; 
	strcpy (aux, synchset);
	strcat (aux, " EOF");
	while (strstr(aux,t.compLex) == NULL){
		sigLex();
	}
}
void checkinput(char *firsts, char *follows)
{
	
	if (strstr(firsts,t.compLex)==NULL) 
	{
		if (strcmp (t.compLex, "EOF")!=0)
		   printf ("Linea: %d. No se esperaba: %s, se esperaba: %s \n", numLinea, t.compLex, firsts);
		//error2 (firsts);
		int cant = strlen (firsts)+strlen (follows);
		char aux[cant+1];
		strcpy (aux, follows);
		strcat (aux," ");
		strcat (aux,firsts);
		scanto (aux);
	}
}

void match (char *expToken)
{
	
	if (strcmp(t.compLex, expToken)==0){
		sigLex();
	}	
	else
	{
		//error2("Error matching");
		printf ("Linea: %d. Error Matching: Encontrado: %s, se esperaba: %s \n", numLinea, t.compLex, expToken);
		
	}
}

char * element_list1 (char *synchset)
{
	char *primero = "COMA";
	//checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "COMA")==0){
			match ("COMA");
			char * element_trad = element("EOF COMA R_CORCHETE");
			char * element_list1_trad = element_list1("R_CORCHETE");
			char *aux = "\n";
			int cant = strlen (element_trad) + strlen (element_list1_trad) + strlen (aux);
			char trad [cant];
			strcpy (trad, aux);
			strcat (trad, element_trad);
			strcat (trad, element_list1_trad);
			char * trad2 = trad;
			//printf (trad2);
			return trad2;
		}
		
		checkinput(synchset, primero);
		return "";
	}
}

char * element_list (char *synchset)
{
	char *primero = "L_CORCHETE LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "LITERAL_CADENA")==0 || strcmp (t.compLex, "L_CORCHETE")==0){
			char * element_trad = element("EOF COMA R_CORCHETE");
			char * element_list1_trad = element_list1("R_CORCHETE");
			int cant = strlen (element_trad) + strlen (element_list1_trad);
			char trad [cant];
			strcpy (trad, element_trad);
			strcat (trad, element_list1_trad);
			char * trad2 = trad;
			return trad2;
		}
		else 
			error2 ("element_list1");
			
		checkinput(synchset, primero);
		return "";
	}
}

char * attribute_value (char *synchset)
{
	char *primero = "LITERAL_CADENA LITERAL_NUM PR_TRUE PR_FALSE PR_NULL";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "LITERAL_CADENA")==0){
			char * cad = t.pe->lexema; 
			match ("LITERAL_CADENA");
			//printf (cad);
			return cad;
		}
		else if (strcmp (t.compLex, "LITERAL_NUM")==0){
			char * num = t.pe->lexema;
			match ("LITERAL_NUM");
			int cant = strlen (t.pe->lexema) + 2;
			char trad [cant];
			strcpy (trad, "\"");
			strcat (trad, num);
			strcat (trad, "\"");
			char * aux = trad;
			return aux;
		}
		else if (strcmp (t.compLex, "PR_TRUE")==0){
			match ("PR_TRUE");
			return "true";	
		}
		else if (strcmp (t.compLex, "PR_FALSE")==0){
			match ("PR_FALSE");
			return "false";	
		}
		else if (strcmp (t.compLex, "PR_NULL")==0){
			match ("PR_NULL");
			return "";	
		}
		else 
			error2 ("attribute_value");
		checkinput(synchset, primero);
		return ""; //inv
	}
}


char * attribute_name(char *synchset)
{
	char *primero = "LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "LITERAL_CADENA")==0){
			char * cad = t.pe->lexema; 
			match ("LITERAL_CADENA");
			//return (SC (cad));
			return ((cad));
		}
		else 
			error2 ("atributte_name");
		checkinput(synchset, primero);
		return ""; //inv
	}
}


char * attribute(char *synchset)
{
	char *primero = "LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "LITERAL_CADENA")==0){
			char * attribute_name_trad = attribute_name("DOS_PUNTOS");
			match ("DOS_PUNTOS");
			char *attribute_value_trad = attribute_value("COMA R_LLAVE");
			int cant = strlen (attribute_name_trad) + strlen (attribute_value_trad)+ 2;
			char trad [cant];
			strcpy (trad, attribute_name_trad);
			strcat (trad, "= ");
			strcat (trad, attribute_value_trad);
			char * trad2 = trad;
			return trad2;
		}
		else 
			error2 ("attribute");
		checkinput(synchset, primero);
		return "";//inv
	}
}

char * attributes_list (char *synchset)
{
	char *primero = "LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "LITERAL_CADENA")==0){
			char * attribute_trad = attribute("COMA R_LLAVE");
			char * attributes_list1_trad = attributes_list1("R_LLAVE");
			int cant = strlen (attribute_trad) + strlen (attributes_list1_trad);
			char trad [cant];
			strcpy (trad, attribute_trad);
			strcat (trad,attributes_list1_trad);
			char * attributes_list_trad = trad;
			return attributes_list_trad;
		}
		else
			error2 ("atributtes_list");
		checkinput(synchset, primero);
		return "";//inv
	}
}

char * attributes_list1 (char *synchset)
{
	char *primero = "COMA";
	//checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "COMA")==0){
			match ("COMA");
			char * attribute_trad = attribute("COMA R_LLAVE");
			char * attributes_list1_trad = attributes_list1("R_LLAVE");
			int cant = 1 + strlen (attribute_trad) + strlen (attributes_list1_trad);
			char trad [cant];
			strcpy (trad, " ");
			strcat (trad, attribute_trad);
			strcat (trad, attributes_list1_trad);
			char * attributes_list1_tradu = trad;
			return attributes_list1_tradu;
		}
		checkinput(synchset, primero);
		return ("");
	}
}

char * attributes1 (char *synchset)
{
	char *primero = "LITERAL_CADENA";
	//checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "LITERAL_CADENA")==0){
			char * attributes_list_trad = attributes_list("R_LLAVE");
			return attributes_list_trad;
		}
		checkinput(synchset, primero);
		return "";
	}
}

char * attributes (char *synchset)
{	
	char *primero = "L_LLAVE";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "L_LLAVE")==0){
			match ("L_LLAVE");
			char * attributes1_trad = attributes1("R_LLAVE");
			match ("R_LLAVE");
			return attributes1_trad;
		}
		else
			error2 ("attributes");
		checkinput(synchset, primero);
		return "";//inv
	}
}

char * tag_name (char *synchset)
{
	char *primero = "LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp (t.compLex, "LITERAL_CADENA")==0){
			char * tag_name_trad = t.pe->lexema;
			//printf ((tag_name_trad));
			match ("LITERAL_CADENA");
			//return (SC (tag_name_trad));
			return ((tag_name_trad));
		}
		else
			error2 ("tag_name");
		checkinput(synchset, primero);
	//	return "";//inv
	}
}

char * element3 (char *synchset)
{
	char *primero = "COMA";
	//checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		match ("COMA");
		char * element_list_trad = element_list("R_CORCHETE");
		int cant = 1 + strlen (element_list_trad);
		char trad [cant];
		strcpy (trad, ",");
		strcat (trad, element_list_trad);
		char * element3_trad = trad;
		return element3_trad;
	}
	checkinput(synchset, primero);
	return "";
}

char * element2 (char *synchset)
{
	char *primero = "L_LLAVE L_CORCHETE LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp(t.compLex, "L_LLAVE")==0){
			char * attributes_trad = attributes ("COMA R_CORCHETE");
			char * element3_trad = element3 ("R_CORCHETE");
			int cant = 2 + strlen (attributes_trad) + strlen (element3_trad);
			char trad [cant];
			strcpy (trad, " ");
			strcat (trad, attributes_trad);
			strcat (trad, ">");
			strcat (trad, element3_trad);
			char * element2_trad = trad;
			return element2_trad;
		}
		else if ((strcmp(t.compLex, "L_CORCHETE")==0) || strcmp(t.compLex, "LITERAL_CADENA")==0){
			char *element_list_trad = element_list ("R_CORCHETE");
			char * aux = "\n";
			int cant = strlen (aux) + strlen (element_list_trad);
			char trad [cant];
			strcpy (trad, aux);
			strcat (trad, element_list_trad);
			char* element2_trad = trad;
			return element2_trad;
		}
		else
			error2("Element2");
		checkinput(synchset, primero);
		return "";//inv
	}
}

char * element1 (char *synchset)
{
	char *primero = "COMA";
	//checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp(t.compLex, "COMA")==0)
		{
			match ("COMA");
			char * element2_trad = element2 ("R_CORCHETE");
			return element2_trad;
		}
		char * aux = (" >"); 
		checkinput(synchset, primero);
		printf (aux);
		return aux;
	}
}
char * element (char *synchset)
{
	char *primero = "L_CORCHETE LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		if (strcmp(t.compLex, "L_CORCHETE")==0)
		{
			match ("L_CORCHETE");
			char * tag_name_trad = "";
			tag_name_trad = tag_name("COMA R_CORCHETE");
			//printf (tag_name_trad);
			char * element1_trad = element1 ("R_CORCHETE");
			//printf (element1_trad);
			match ("R_CORCHETE");
			int cant = 1+ strlen (tag_name_trad) + 1 + strlen (element1_trad) + 5 + strlen (tag_name_trad);
			char trad [cant];
			strcpy (trad, "<");
			strcat (trad, tag_name_trad);
			strcat (trad, " ");
			strcat (trad, element1_trad);
			strcat (trad, "</ \" ");
			strcat (trad, tag_name_trad);
			strcat (trad, "\" >");
			char *element_trad = trad;
			//printf (trad);
			return element_trad;
		}
		else if (strcmp(t.compLex, "LITERAL_CADENA")==0){
			char * cad = t.pe->lexema;
			match ("LITERAL_CADENA");
			return cad;
		}
		else error2("Error Element");
     	checkinput(synchset, primero);
     	return "";//inv?;
	}
}

char * jsonml (char *synchset)
{
	char *primero = "L_CORCHETE LITERAL_CADENA";
	checkinput (primero,synchset);
	if (strstr(synchset,t.compLex)==NULL){
		char * json = element("EOF COMA R_CORCHETE");
		//printf (json);
		return json;
	}
	else error2("Jsonml");
	checkinput(synchset, primero);
	return "";//inv

}

int main(int argc,char* args[])
{
	// inicializar analizador lexico
	char *complex;
	int linea=0;
	initTabla();
	initTablaSimbolos();
	archSalida=fopen("Salida.txt","w");
	if(argc > 1)
	{
		if (!(archivo=fopen(args[1],"rt")))
		{
			printf("Archivo no encontrado.\n");
			exit(1);
		}
		sigLex();
		char * texto = jsonml("EOF");
		/*while (t.compLex!="EOF"){
			sigLex();
			//jsonml("EOF");
			
			int num_anterior;
			if (t.compLex!="-1")
			{	if(numLinea>linea)
				{	printf("\n%d: %s ",numLinea,t.compLex);
					fprintf(archSalida,"\n%d: %s ",numLinea,t.compLex);
					linea = numLinea;
				}
				else
				{	printf("%s ",t.compLex);
					fprintf(archSalida,"%s ",t.compLex);
				}
			}
		}
		fclose(archivo);*/
		printf ("\nFin Compilacion. Lineas analizadas: %d", numLinea);
		printf ("\n-------------  TRADUCCION XML  --------------\n");
	//	printf (texto);
	}else{
		printf("Debe pasar como parametro el path al archivo fuente.\n");
		exit(1);
	}

	return 0;
}

