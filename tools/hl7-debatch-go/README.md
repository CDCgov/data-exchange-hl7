### Tool only 

Debatch for HL7 batch messages in Go.

### How to Use:

- Download and install Go:
  [https://go.dev/doc/install](https://go.dev/doc/install)

```Go
$ go run main.go  
```


### Test Results

|HL7 Messages |File Size | Time to Debatch |
---             | ---      | ---         |
|     20,000    | 74 MB    | 0.3 sec     |
|    200,000    | 740 MB   | 2.1 sec     |
|  2,000,000    | 7.45 GB  | 21 sec      |
| 10,000,000    | 37.26 GB | 1min 50 sec |

Using 1 Machine: 3.4GHz 16 Core CPU, 64GB RAM