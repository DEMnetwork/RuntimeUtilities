# RuntimeUtilities
A Library that allows you to Allocate Off-Heap Memory, Inject Dependancies to JARs, etc.

## How to inject dependancies

Currently, the implemented DI is Class-Level via [`io.github.demnetwork.runtime/io.github.demnetwork.runtime.inject.InjectedClassLoader`](src/io.github.demnetwork.runtime/io/github/demnetwork/runtime/inject/InjectedClassLoader.java);
