## Como usar el creador de clases

Una vez bajado el código fuente, ejecutaremos el mismo pasandole 2
parámetros:

-   1: La ruta al script SQL
-   2: La ruta donde guardar el modelo

El programa automaticamente creará y mapeara el modelo.

## LIMITACIONES

Solo he probado a generar el modelo con el programa [SQLEditor de
Mac][], por lo que si obtienes el script sql de otro programa, puede no
funcionar ;) El reconocimiento de foreign keys es mediate el nombre del
campo, es decir, solo se reconocen foreign keys con el nombre:
“id\_campo” o “campo\_id”, si sigues esa estructura no debes tener
problemas. Si en tu bdd se usa otro sufijo o prefijo, puedes cambiar el
código fuente.

## How to use the class creator

The program needs two params for run correctly:

-   1: Path to SQL script
-   2: Path to a folder for save dom.

The program creates and gives mapping automatically.

## LIMITATIONS

I only tested this with [SQLEditor for Mac][SQLEditor de Mac]. If you
get the SQL script of a another program, maybe it does not work ;). The
foreign keys are mapped by the field name. It only recognizes the field
name with prefix “id\_fieldname” or suffix “fieldname\_id”. If your
database names are distinct please feel free to change the names if
source code. My bad english but… this is another history… :D

  [SQLEditor de Mac]: http://www.malcolmhardie.com/sqleditor/
