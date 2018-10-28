Release Notes Knopflerfish $(VERSION) (OSGi R6)
======================================================================

Maintenance release of Knopflerfish 6 available from
$(BASE_URL)/$(VERSION). Released $(RELEASE_DATE).

Knopflerfish 6 is an implementation of the "OSGi Service Platform
Release 6". It contains all services specified in the "Core
Specification" and most of the non Enterprise Edition related
services specified in the "Compendium Specification".

The Release Notes include all new features & changes for
Knopflerfish $(VERSION) compared to the release of Knopflerfish
$(VERSION_PREV)

Knopflerfish Framework - OSGi Core Specification
----------------------------------------------------------------------

### Framework 8.0.8

* Fixed issue #47. reference:file: bundles can not be loaded if
  org.knopflerfish.osgi.registerserviceurlhandler=false

### Framework 8.0.7

* Fixed issue #44. Avoid NoSuchElementException in bundle classloader
  when doing getResource.

### Framework 8.0.6

* Fixed issue #40. There is a small risk for a dead lock if you
  dynamically import a package and at the same time resolve a bundle
  that access the same package.


OSGi Compendium Specification
----------------------------------------------------------------------


Knopflerfish Services
----------------------------------------------------------------------

### Crimson is no longer redistributed

* The Knopflerfish redistribution of crimson has been removed.



Misc, start scripts, build system etc 
----------------------------------------------------------------------

### Build with Java 9 / JDK9

* Updated the build system to support building with JDK9 with the following
  remarks / limitations:
    - Compact version of framework is not built since the proguard compactor
      does not support Java 9
      - Updated asm to version 6.0. Older versions does not support Java 9.
      - Crimson does not support Java 9 and has been removed.
  
### asm 6.0

* asm has been updated to version 6.0, with support for Java 9

### Knopflerfish installer - jarunpacker

* Updated to support building with JDK9

### Maven snapshot bundles / artifacts

* In a Knopflerfish snapshot release the release specific maven2 repo
  will be a snapshot repository, i.e. include snapshot versions of
  all bundles only.

### Documentation

* Corrected broken links, causing 404 Not Found error
