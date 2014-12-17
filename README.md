This code is a modified version from the revision 9 of javaemvreader(https://code.google.com/p/javaemvreader/)
for building android-se-access(https://github.com/nelenkov/android-se-access)

if you build the android-se-access project with the latest version of javaemvreader, you may get theerror messages like 
	"Error:(4, 16) java: cannot find symbol
	: class ApplicationDefinitionFile
	location: package sasc.emv"
	"Error:(8, 16) java: cannot find symbol
  	:   class EMVCard
  	location: package sasc.emv"

I digged out amost all revisions of javaemvreader svn repository and found out 
that the revision 9 has the fewest errors in binding to android-se-access.
I modified and added a few lines to the revision 9.

>diff javaemvreader-r9/src/main/java/sasc/emv/EMVAPDUCommands.java javaemvreader/src/main/java/sasc/emv/EMVAPDUCommands.java
>55a56,59
>
>     public static String selectPPSE() {
>         return selectByDFName(Util.fromHexString("32 50 41 59 2E 53 59 53 2E 44 44 46 30 31")); //2PAY.SYS.DDF01
>     }
> 
>diff javaemvreader-r9/src/main/java/sasc/emv/EMVCard.java javaemvreader/src/main/java/sasc/emv/EMVCard.java
>44a45
>
>     private Type type = Type.CONTACTED;
>
>53a55,66
>
>     public ATR getATR(){
> 	return this.atr;
>     }
> 
>     public void setType(Type type){
>         this.type = type;
>     }
> 
>     public Type getType() {
> 	return this.type;
>     }
> 

You can build javaemvreader to use maven.
> sudo mvn package

Apache License:
http://www.apache.org/licenses/LICENSE-2.0


