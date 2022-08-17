## How to Use:

- Download and install Go:
  [https://go.dev/doc/install](https://go.dev/doc/install)

```Go
// this will produce the large file, and it may take a while depending on the system is running on

$ go run main.go  

```

```Go
//   20,000 HL7 batched messages  ->    74 MB file size	   	    -> Az explorer upload    7 sed 			   ->      0.3 sec debatch local desktop
//    200,000 HL7 batched messages  ->   740 MB (0.7 GB) file size  -> Az explorer upload   28 sec   		   ->      2.1 sec debatch local desktop
//  2,000,000 HL7 batched messages  ->  7.45 GB file size 		    -> Az explorer upload  520 sec (8.7 min)   ->      21  sec debatch local desktop
// 10,000,000 HL7 batched messages  -> 37.26 GB file size 		    -> Az explorer upload 1,227 s (20.45 min)  -> 1min 50  sec debatch local desktop
```