# Guns & NPCs
*A proof-of-concept(s) game made in Java*


Notable implementations include:

- Enemy NPCs
  - A* Graph Traversal Algorithm
    - Allows for NPCs to move in a dynamic, randomly produced environment.
    - 4-direction implementation with Euclidean distance acting as heuristic.
  - Behavior
    - Line-of-Sight Raycasting
      - NPCs perform raycasts to their target to determine if they should fire or attempt to path into a position where they can fire.
- Bitmap Entity Management
  - ID Pool efficiently keeps track of class instances.
  - Use of HashMap
    - Infrequent rehashing with allowance of null "holes", allows bitmap to be the way it is.
    - Pre-optimized HashMaps (initial capacity and load factor) mitigate internal rehashing with volatile maps.
    - Allow for arbitrary access in various data structures used by the application, especially with respect to graph traversal.
  - Entity Collision
    - All game entities registered with the bitmap can collide in two-dimensional space.
    - Basic intersection calculations done on entities.
    - Cell based entity implementation avoids using potentially exhaustive iteration on each frame.
 

This repository features all the source code in the Source Code folder, a compiled version called Guns & AI.jar, and this README.
The source code in the repository was largely created in May and June of 2016.
