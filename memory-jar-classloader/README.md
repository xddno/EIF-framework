This is the classloader used to load InjectableJar

It's a custom classloader that takes a byte array as input

It will also change the class loasing delegation model for the asm library, \
to make sure we use the one embedded in InjectableJar, and not the one already in the game we inject into.