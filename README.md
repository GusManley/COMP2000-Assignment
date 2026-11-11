# COMP2000 Assignment

## Make a Simulation
Through this semester, we will be making a simulation. Our idea is to center it around a pandemic, while incorporating some game-ified elements to make it interesting. By having a rock-paper-scissors type of interaction between nodes, we will simulate how an infection can spread between healthy people, and how an antidote can cure those infected.

## Group TODO
https://docs.google.com/spreadsheets/d/12h55ryoM3mye0ntDo29wyEReM5MRKOBxwy7ihyzLNBk/edit?gid=0#gid=0

## Group Name: Due Tomorrow

## System mechanics

### The system composed of 3 cell types repressented by different colors

- RED - Infected cell
- GREEN - Neutral cell
- BLUE - Antivirus cell

### The system also has dfferent seasons which has special effect to certain cell types

- SUMMER - Infected cells are 1.2x faster for the season duration

- AUTUMN - Antivirus cells are 1.2x larger for the season duration
 
- WINTER - Every second each cell has a 3% chance to die

- SPRING - Every second each neutral cell has a 10% chance to reproduce

### User can interact with the system through settings panel
Settings panel allow user to set specific interactions such as:
- Cell size 
- Cell speed 
- Total cell count
- Number of infected cell
- Number of Antivirus cell
- Toggle collision between cells (on/off)

## Special effect for each cell type
- Infected Cell
    - BERSERK - infected cells can enter berserk when low in number. When berserk every cells it collided with will turn to infected cell and berserk cell gains immunity
        - Curently have 5 berserk chances
        - berserk lasts 10 secs
        - Indicated by infected cells have yellow aura pulsing
    - RESISTANCE - Infected cells can gain resistance to anti-viruses, increasing the chance of their survival
- Antivirus Cell
- Neutral Cell
    - IMMUNITY - Neutral cells simulate adaptive immunity through the Cell Immunity feature. This grants a chance for a neutral cell to become immune to an infected cell temporarily. 
