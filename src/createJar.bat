set src="C:/Users/Sergey/Desktop/java advanced/src"
set path_implementor=ru.ifmo.rain.balahnin.implementor.Implementor.jar
set classpath_implementor="ru/ifmo/rain/balahnin/implementor"
set kgeorgiy="info/kgeorgiy/java/advanced/implementor"

cd %src%

jar xf info.kgeorgiy.java.advanced.implementor.jar %kgeorgiy%/Impler.class %kgeorgiy%/JarImpler.class %kgeorgiy%/ImplerException.class
jar cvfm %path_implementor% ru/ifmo/rain/balahnin/implementor/MANIFEST.MF %kgeorgiy%/Impler.class %kgeorgiy%/JarImpler.class %kgeorgiy%/ImplerException.class %classpath_implementor%/Implementor.class"
