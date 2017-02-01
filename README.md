
SOFTWARE REQUIREMENTS
 - Java 1.7 or later

COMPILATION AND RUNNING

 - Run 'javac Main.java' in the terminal to compile the program.
 - Run 'java Main' to start the program.

CONFIGURATION GUIDE
 - The 'config.properties' file contains parameters that are required to run the program.
 
        'bootstrap.ip'     : IPv4 address of the bootstrap server
        'bootstrap.port'   : Port number of the bootstrap server
        'hops.max'         : Maximum hops allowed
        'node.ip'          : IPv4 address of the node
        'node.port'        : Port number of the node (Unset this parameter to pick a random port)
        'node.username'    : Username used to register with bootstrap server
        'file.list'        : Name of the text file which contains file names. 
                             Must located where the Main class is present (default is 'file_names.txt')
        'listener.timeout' : Timeout of the message listener.

 - The file_names.txt is used for store file list. Each file name should start in a new line.


TEAM MEMBERS 

120006T - R.H.N.M.R.L. Abeysekara
120010B - S.W. Abeywardena
120195T - P.R.B. Harasgama
120342T - L.R.A.L. Liyanage