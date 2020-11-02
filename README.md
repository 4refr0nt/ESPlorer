# ESPlorer

[![Build Actions Status](https://github.com/4refr0nt/ESPlorer/workflows/build/badge.svg)](https://github.com/4refr0nt/ESPlorer/actions) [![Join the chat at https://gitter.im/4refr0nt/ESPlorer](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/4refr0nt/ESPlorer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
#### Integrated Development Environment (IDE) for ESP8266 developers

### Package Description
The essential multiplatforms tools for any ESP8266 developer from luatool author’s, including a LUA for NodeMCU and MicroPython. Also, all AT commands are supported.

Requires Java 8 or above.
Download the latest and greatest one from [Oracle website](https://www.oracle.com/java/technologies/javase-downloads.html).

### Supported platforms
- Windows(x86, x86-64)
- Linux(x86, x86-64, ARM soft & hard float)
- Solaris(x86, x86-64)
- Mac OS X(x86, x86-64, PPC, PPC64)

### Detailed features list
- Syntax highlighting LUA and Python code
- Code editor color themes: default, dark, Eclipse, IDEA, Visual Studio
- Undo/Redo editors features
- Code Autocomplete (Ctrl+Space)
- Smart send data to ESP8266 (without dumb send with fixed line delay), check correct answer from ESP8266 after every line.
- Code snippets
- Detailed logging
- and more, more more…

### Discuss
* [English esp8266.com](http://www.esp8266.com/viewtopic.php?f=22&t=882)
* [Russian esp8266.ru](http://esp8266.ru/forum/threads/esplorer.34/)

### Home Page
[http://esp8266.ru/ESPlorer/](http://esp8266.ru/esplorer/)

### Latest binaries download
Check out [Releases](https://github.com/4refr0nt/ESPlorer/releases)

### Build from sources
#### Windows
```
mvnw.cmd clean package
```
#### Linux / Mac OS
```
./mvnw clean package
```
The build creates all-in-one executable `ESPlorer.jar` in the `target` folder.
Then run the app:
```
java -jar target/ESPlorer.jar
```
