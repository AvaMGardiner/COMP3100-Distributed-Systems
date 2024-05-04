# Stage 1: Demo Instructions

Usage:

./demoS1.sh [Java specific argument...] [-n] your_client.class [your client specific argument...]

This test suite is what will be used for your demo. To untar, use "tar -xvf S1Demo.tar". 

A detailed demo procedure will be available a bit later.

The Java specific argument part of the script ("[Java specific argument...]") is for the Java runtime, such as the classpath ("-cp").

If you're having trouble running the script, check its permissions, the executable one for the owner.

------------------------------------------------------------------------------------

The demo is for you to basically compile and run your client with the test suite provided (the script and configuration files). 

The detailed demo procedure is as follows:



At the beginning of class

1. Get your Ubuntu VM up and running

2. Get your test environment ready; for example, you may place your client (the .java file) and any necessary files for the demo in ds-sim/src/pre-compiled/ in which ds-server and ds-client reside. 

3. Test out if your client is working as expected

4. Get your report open

5. Get your git repository open



At the start of each demo in front of the marker (Remember, you're given a max of 4mins depending on the number of students in a particular workshop)

1. Show the content of demo directory (ds-sim/src/pre-compiled/)

2. Remove the test suite

3. Download the test suite from iLearn and untar it in the demo directory, e.g., tar -xvf S1Demo.tar

4. Download/clone your git repository

5. Upon your marker's signal, compile your client

6. Run the script; while running the tests, show your report if the URL of your git repo is there, and then show your git repository (commit history in particular) to demonstrate the practice of good project management

7. Confirm test results; (briefly) discuss results if necessary
