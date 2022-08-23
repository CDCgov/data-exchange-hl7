import datetime

start_time = datetime.datetime.now()

f = open("batch.txt", "r") 

index = 0

messages = []
current_lines = []

lines = f.readlines()

for line in lines:
    if not( line.startswith("FHS") or line.startswith("BHS") or line.startswith("BTS") or line.startswith("BHS")):
        if line.startswith("MSH"):
            if index > 0:
                # print("index: " + str(index))
                messages.append("".join(current_lines))

            index = index + 1
            # empty for new message
            current_lines = []
        # print("appending: " + line) 
        current_lines.append(line) 

# append last message
messages.append("".join(current_lines))


print("File has " + str(len(messages)) + " HL7 messages")
print("Run duration", end=" ")
print(datetime.datetime.now() - start_time)
# print(messages)