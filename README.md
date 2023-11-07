
# Fundraiser Project - User Manual
Project 2 for CSCI 455 NDSU

Uses UDP protocol for communication

## Prerequisites

 - [FundraiserServerUDP.jar](https://github.com/ethanseverson/csci455project2/releases/download/v2.0/FundraiserServerUDP.jar)
 - [FundraiserClientUDP.jar](https://github.com/ethanseverson/csci455project2/releases/download/v2.0/FundraiserClientUDP.jar)

*Java JDK is required to run Java applications, it is  recommended to use Java JDK 16 or newer.*

**Anytime the program is requesting input from you (the user), you will be hinted with `>>` in the console.**

## Starting the Server

In a terminal environment, run `java -jar FundraiserServerUDP.jar`

You will be prompted to to enter a server port:
```text
Enter the server port:
>>
```
Make sure the port you are using is available and not reserved for another program. For example, enter `6816`, as this is not a common port used by other applications. More information for checking commonly used ports can be found [here](https://www.speedguide.net/ports.php).

If the server shows the following, your server is successfully set up. (Port number will vary)
`The UDP server is running. Listening for datagrams on port 6812.`

If you are planning to let other computers connect, you may need to configure your firewall settings depending on your configuration.

## Additional Server Information reguarding UDP

Since UDP doesn't hold a connection, the server will track sessions by IPv4 address. Due to this limitation, only one instance is allowed per IP address (You can't run two clients on the same PC). Depending on your NAT and internet configuration, in most cases only 1 client per home network can be used with the server as all devices share the same public IPv4 address. (Some internet providers use CG-NAT, so you might be sharing a public IPv4 address with another user).

When the server receives a message, it will map and pass the message to a ClientInstance. Each ClientInstance holds the flow and sequence of menu interactions that the user has requested. Another limitation of UDP is not guaranteeing the message is successfully sent. If the client doesn't see a response from the server within 5 seconds, the client will ask the user to reconnect. 

The server supports resuming a session with the client. This allows the menu content and options to be retransmitted to the user, so the user can pick up where they left off. If the user ungracefully leaves, the server won't know that the user has disconnected, so a cleanup server will check every 2 minutes the age of a ClientInstance. If a ClientInstance hasn't been interacted with in the last 30 minutes, the session will close and the user will be notified their session has ended.

## Server Log

As the program is being used, you can check the console of which the server is running to see what events have been taking place.  A few example lines are shown below. (Also notice how the threads are reused after a user logs off)
```
[2023-11-07 02:22:38 PM] <main> Datagram received from /127.0.0.1:52580 with message: create
[2023-11-07 02:22:38 PM] <pool-1-thread-1> Handling datagram from /127.0.0.1:52580 with message: create
[2023-11-07 02:22:38 PM] <pool-1-thread-1> Handling message for /127.0.0.1:52580
[2023-11-07 02:22:38 PM] <pool-1-thread-1> ClientInstance found for /127.0.0.1:52580
[2023-11-07 02:22:38 PM] <pool-1-thread-1> /127.0.0.1:52580 >> Message received from client: create
[2023-11-07 02:22:38 PM] <pool-1-thread-1> Sent message to clientInstance: create
[2023-11-07 02:22:38 PM] <pool-2-thread-1> /127.0.0.1:52580 >> Waiting for response.
[2023-11-07 02:22:38 PM] <pool-2-thread-1> /127.0.0.1:52580 >> User starting to create fundraiser.
[2023-11-07 02:23:09 PM] <pool-3-thread-1> Running cleanup service.
[2023-11-07 02:23:09 PM] <pool-3-thread-1> 1 sessions open, no sessions were closed.
```

## Connecting the Client

In a terminal environment, run `java -jar FundraiserClientUDP.jar`

You will be prompted to enter the IP and port number of the remote server. This example below is an example of connecting to localhost (the server running on the same PC as the client)

> Successful Connection

```text
Enter the server IP:
>> 127.0.0.1
Enter the port:
>> 6816
Connecting to server...
Type exit or quit at any time to disconnect.
You have started a new session on the server. The current time is 02:22:37 PM
```
Successful connections will be greeted with a session message and the current time.
> Successful Connection, resuming session

Since UDP doesn't hold a connection, the server will track your session by your IPv4 address. If opening a new instance of the client, your session will be recognized.
```
Enter the server IP:
>> 127.0.0.1
Enter the port:
>> 6812
Type exit or quit at any time to disconnect.
It appears you already have a session open, last interacted with 6 minutes ago.
Would you like to resume this session? (yes/no)
>> yes
There is where you left off. Session resumed.
...
```
or if you prefer a new session, you can do that as well
```
Enter the server IP:
>> 127.0.0.1
Enter the port:
>> 6812
Type exit or quit at any time to disconnect.
It appears you already have a session open, last interacted with 1 minutes ago.
Would you like to resume this session? (yes/no)
>> no
Old session terminated.
You have started a new session on the server. The current time is 02:31:33 PM
...
```

> Unsuccessful Connection

UDP doesn't hold a connection, so the client will notify if no response is received within 5 seconds from the server, the client will notify.
```text
Enter the server IP:
>> 127.0.0.1
Enter the port:
>> 6900
Type exit or quit at any time to disconnect.
```
5 seconds later...
```
No response received within 5 seconds. Please check if the server IP & port are correct then try again.
Enter the server IP:
>> 
```
If specifying a IP/port that is not listening, the program will attempt 4 times then exit.
>Invalid IP/Port

```text
>> 10.0.0
Invalid IP address. Please enter a valid IPv4 address.
>> 260.0.0.1
Invalid IP address. Please enter a valid IPv4 address.
>>
```
```text
>> 80000
Invalid port number. Please enter a number between 0 and 65535.
>>
```
If the IP entered is not a valid IPv4 address or the port number is not valid, you will be prompted to try again.

## Navigating the menus
The functionality of the project is the same as from project 1.

Upon connecting to the server, you will see the main menu. The main menu will display the current fundraisers along with some options.

A new instance of the server will start at a fresh state with nothing stored, as shown.
```text
+------------------------------------+
|        Current Fundraisers         |
+------------------------------------+
|  There are no current fundraisers  |
+------------------------------------+
Type "create" to create a new fundraiser.
>>
```
The only option available at this time will be to create a fundraiser. 

If there are already fundraisers, you will be shown a table with the current fundraisers sorted by deadline. This table will show you the index number, used for navigating to that fundraisers donation view, event name, total amount raised, target amount, deadline, and the donation count.
```text
+----------------------------------------------------------------------------------------------------+
|                                        Current Fundraisers                                         |
+-----+----------------------+-----------------+-----------------+---------------------+-------------+
|  #  |      Event Name      |  Amount Raised  |  Target Amount  |      Deadline       |  Donations  |
+-----+----------------------+-----------------+-----------------+---------------------+-------------+
|  1  |  Firefighter Dinner  |  $0.00          |  $80.00         |  October 28, 2023   |  0          |
+-----+----------------------+-----------------+-----------------+---------------------+-------------+
|  2  |  Church              |  $20.00         |  $200.00        |  October 30, 2023   |  1          |
+-----+----------------------+-----------------+-----------------+---------------------+-------------+
|  3  |  Halloween Candy     |  $0.00          |  $150.00        |  October 31, 2023   |  0          |
+-----+----------------------+-----------------+-----------------+---------------------+-------------+
|  4  |  Food Shelf          |  $0.00          |  $400.00        |  November 05, 2023  |  0          |
+-----+----------------------+-----------------+-----------------+---------------------+-------------+
Type "create" to create a new fundraiser.
Type "past" to view past fundraisers.
Otherwise, type the number corresponding to the fundraiser above to open it.
>>
```
Each fundraiser entry is populated with a number to the left of its name, use that number to navigate to that fundraiser's donation view. For the scenario above, numbers 1-4 are valid inputs, along with the option to view past fundraisers (since past fundraisers exist), and then the option to create a new fundraiser.

## Creating a Fundraiser

From the main menu, type `create`:
```text
...
>> create
Type "cancel" at any time to return back to main menu.
Please enter the name of the fundraiser.
>> Church
Please enter the target amount for the fundraiser. ($)
>> 200
Please enter the deadline for the fundraiser (yyyy-mm-dd).
>> 2023-10-30
+-------------------------------------------------------------------------------------------+
|                                    Current Fundraisers                                    |
+-----+--------------+-----------------+-----------------+--------------------+-------------+
|  #  |  Event Name  |  Amount Raised  |  Target Amount  |      Deadline      |  Donations  |
+-----+--------------+-----------------+-----------------+--------------------+-------------+
|  1  |  Church      |  $0.00          |  $200.00        |  October 30, 2023  |  0          |
+-----+--------------+-----------------+-----------------+--------------------+-------------+
Type "create" to create a new fundraiser.
Otherwise, type the number corresponding to the fundraiser above to open it.
>>
```
As shown above, after requesting to create  a fundraiser, it will ask you for the event name, target amount, and the deadline. Invalid inputs will be caught and the user will be asked to try again.

The fundraiser deadline must be entered in the format of `yyyy-mm-dd`, for example, `2023-01-01` is valid. Invalid examples include `2023-1-1`, `23-1-1`, `01-01-2023`, etc... You must follow the the format provided, including leading 0s.

Deadlines in the past are allowed to be created, but you will receive a warning:
```text
Please enter the deadline for the fundraiser (yyyy-mm-dd).
>> 2023-07-04
Warning: The date you entered is in the past.
```
Fundraisers in the past will not be allowed to receive any donations.

## Fundraiser donation view
From either the current fundraisers or past fundraisers menu, you can type in the number shown in the list to open that fundraiser. For example, if the event "Church" is 2nd in the list, type `2` then press enter to open the fundraiser donation view.
```text
>> 2
+---------------------------------------------------------------+
|              Fundraiser: Church, $20.00 raised.               |
+-----+--------+--------------+----------+----------------------+
|  #  |  Name  |  IP Address  |  Amount  |      Date/Time       |
+-----+--------+--------------+----------+----------------------+
|  1  |  Jack  |  127.0.0.1   |  $20.00  |  10/24/2023 4:02 PM  |
+-----+--------+--------------+----------+----------------------+
Type "back" to go back to fundraisers list.
Type "remove" if you want to delete this fundraiser.
Type "donate" to donate to this fundraiser.
>>
```
You will be presented with a table of donations received for that fundraiser. Each donation will show the name of the person who donated, the IP Address they were connected from, the amount, and finally the date/time the donation was received.

If the fundraiser you viewed is past it's deadline, you will not be able to donate to it. To go back to the fundraisers list, type `back`. If you were viewing a past fundraiser, you will be sent back to the past fundraisers menu, or if you were viewing a fundraiser with a future deadline, you will be sent back to the current fundriasers menu.

To remove a donation, type `remove`. You will need to confirm the deletion by typing `confirm`, as shown:
```text
>> remove
Removing a fundraiser is FINAL. Are you sure you want to continue?.
Type "confirm" if you want to delete, any other input will cancel.
>> confirm
Fundraiser removed.
```

## Donating to a fundraiser

While viewing a fundraisers donation view, type `donate` to add a contribution of your own. You will be asked for your name, then the amount you wish to donate. For demonstration purposes of this assignment, the IP address is also logged for display on the table. Below is an example of a user adding a donation to a fundraiser:
```text
...
>> donate
Type "cancel" at any time to return back to fundraiser.
Please enter your name.
>> Jack
Please enter the amount you wish to donate. ($)
>> 20
Thank you for your donation of $20.00!
+---------------------------------------------------------------+
|              Fundraiser: Church, $20.00 raised.               |
+-----+--------+--------------+----------+----------------------+
|  #  |  Name  |  IP Address  |  Amount  |      Date/Time       |
+-----+--------+--------------+----------+----------------------+
|  1  |  Jack  |  127.0.0.1   |  $20.00  |  10/24/2023 4:02 PM  |
+-----+--------+--------------+----------+----------------------+
```

## Exiting the session

Typing `exit` or `quit` will gracefully close the session that is running on the server. In cases where the client is closed without notifying the server, the server will keep the session open for 30 minutes allowing the user to pick up where they left off. If you intend to do this intentionally, type `pause` or `leave` into the client. The client will ungracefully exit which leaves the server waiting for a future response. If you do not reconnection within 30 minutes, a new session will be created for your IPv4 address.

## Common Problems

 
| Problem | Solution |
|--|--|
|  `Error: Unable to access jarfile FundraiserServerUDP.jar` | Make sure your terminal/command prompt is in the right directory. You might need to change your directory by using a command like `cd`. |
| `Exception in thread "main" java.net.BindException: Address already in use: bind`| This error occurs when another application (or another instance of the FundraiserServerUDP) is running on your computer. Please make sure only one instance of the server is open or change to another port number.|
| `No response received within 5 seconds. Please check if the server IP & port are correct then try again.` | Make sure that the server is running and that you've entered the correct IP and port number. Also, check any firewall config that might be blocking the connection.
