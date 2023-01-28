# Wordle-in-Java
The project consists of the implementation of WORDLE, a web-based word game that went viral in late 2021.
The game consists of finding an English word consisting of 5 letters, taking a maximum number of 12 attempts. WORDLE has a 10-letter word vocabulary, from which it randomly draws a SW word (Secret Word), which users must guess. 

At periodic intervals, a new SW is selected and proposed to all users who log on to the system during that day. 

Then all users must try to guess the secret word. This gives the game a social aspect. The user proposes a GW (Guessed Word) and the system sends the result of the comparison with the SW to the user. If the outcome of the comparison was negative, the system also sends a clue, through colors. 

![ ALT](/images/tips.png)

Finally, a user can share his results on a multicast group. 

![ ALT](/images/notification.png)

## Requirements
<ul>
  <li>Open-jdk | (the game has been developed in v. 17)</li>
</ul>

## Commands

After copying the repositories and moving to the copied folder, run:  <br> <br>
``` java -jar Server.jar ```  - to run the Wordle Server <br> <br>
``` java -jar Client.jar ```  - as many times as you want - to run the Wordle Client <br> <br>
