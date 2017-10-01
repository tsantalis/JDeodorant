# JDeodorant
JDeodorant is an Eclipse plug-in that detects design problems in Java software, known as code smells, and recommends appropriate refactorings to resolve them.

For the moment, the tool supports five code smells, namely **Feature Envy**, **Type/State Checking**, **Long Method**, **God Class** and **Duplicated Code**.

- Feature Envy problems can be resolved by appropriate **Move Method** refactorings.
- Type Checking problems can be resolved by appropriate **Replace Conditional with Polymorphism** refactorings.
- State Checking problems can be resolved by appropriate **Replace Type code with State/Strategy** refactorings.
- Long Method problems can be resolved by appropriate **Extract Method** refactorings.
- God Class problems can be resolved by appropriate **Extract Class** refactorings.
- Duplicated Code problems can be resolved by appropriate **Extract Clone** refactorings.

JDeodorant is the outcome of research conducted in the [Software Refactoring Lab](http://users.encs.concordia.ca/~nikolaos/) at the Department of Computer Science and Software Engineering, Concordia University, Canada
and the [Software Engineering Group](http://se.uom.gr/) at the Department of Applied Informatics, University of Macedonia, Thessaloniki, Greece.

# Installation & Configuration
Please follow the steps described in this [guide](http://users.encs.concordia.ca/~nikolaos/jdeodorant/files_JDeodorant/JDeodorant_Installation_Guide.pdf).

JDeodorant can be easily installed in your Eclipse IDE using the **Eclipse Marketplace Client**.

To enable the analysis of large Java projects, edit the **eclipse.ini** file inside the Eclipse installation folder and increase the value for the **Xmx** option (maximum size of the memory allocation pool).
```
-vmargs
-Xms128m
-Xmx4096m
-XX:PermSize=128m
```

# Tutorials
###### Duplicated Code
- [JDeodorant: Refactoring of the Clones - Teaser Trailer](https://www.youtube.com/watch?v=_WPtgG6JwJ8)
- [JDeodorant: Clone Refactoring (ICSE 2016 Tool demo)](https://www.youtube.com/watch?v=K_xAEqIEJ-4)

###### Code Smell Visualization
- [JDeodorant: Code Smell Visualization Demo](https://www.youtube.com/watch?v=LtH8uF0epV0)

###### God Class
- [JDeodorant: Extract Class Refactoring](https://www.youtube.com/watch?v=h8K2M-lbDYo)

# Research
If you are interested to learn how exactly JDeodorant works, please have a look at the following **papers**:
###### Duplicated Code
- Nikolaos Tsantalis, Davood Mazinanian, and Shahriar Rostami, "[Clone Refactoring with Lambda Expressions](https://users.encs.concordia.ca/~nikolaos/publications/ICSE_2017.pdf)," 39th International Conference on Software Engineering (ICSE'2017), Buenos Aires, Argentina, May 20-28, 2017.
- Nikolaos Tsantalis, Davood Mazinanian, and Giri Panamoottil Krishnan, "[Assessing the Refactorability of Software Clones](http://users.encs.concordia.ca/~nikolaos/publications/TSE_2015.pdf)," IEEE Transactions on Software Engineering, vol. 41, no. 11, pp. 1055-1090, November 2015.
- Giri Panamoottil Krishnan, and Nikolaos Tsantalis, "[Unification and Refactoring of Clones](http://users.encs.concordia.ca/~nikolaos/publications/CSMR-WCRE_2014.pdf)," pp. 104-113, IEEE Conference on Software Maintenance, Reengineering and Reverse Engineering (CSMR-WCRE'2014), 2014 Software Evolution Week, Antwerp, Belgium, February 3-7, 2014.
- Giri Panamoottil Krishnan, and Nikolaos Tsantalis, "[Refactoring Clones: An Optimization Problem](http://users.encs.concordia.ca/~nikolaos/publications/ICSM_2013.pdf)," pp. 360-363, 29th IEEE International Conference on Software Maintenance (ICSM'2013), ERA Track, Eindhoven, The Netherlands, September 22-28, 2013.
- Davood Mazinanian, Nikolaos Tsantalis, Raphael Stein, and Zackary Valenta, "[JDeodorant: Clone Refactoring](http://users.encs.concordia.ca/~nikolaos/publications/ICSE_2016.pdf)," 38th International Conference on Software Engineering (ICSE'2016), Formal Tool Demonstration Session, Austin, Texas, USA, May 14-22, 2016.
- Nikolaos Tsantalis, "[JDeodorant: Clone Refactoring Support beyond IDEs](http://blog.ieeesoftware.org/2016/10/jdeodorant-clone-refactoring-support.html)," IEEE Software Blog, October 16, 2016.

###### Long Method
- Nikolaos Tsantalis, and Alexander Chatzigeorgiou, "[Identification of Extract Method Refactoring Opportunities for the Decomposition of Methods](http://users.encs.concordia.ca/~nikolaos/publications/JSS_2011.pdf)," Journal of Systems and Software, vol. 84, no. 10, pp. 1757–1782, October 2011.
- Nikolaos Tsantalis, and Alexander Chatzigeorgiou, "[Identification of Extract Method Refactoring Opportunities](http://users.encs.concordia.ca/~nikolaos/publications/CSMR_2009.pdf)," pp. 119-128, 13th European Conference on Software Maintenance and Reengineering (CSMR'2009), Kaiserslautern, Germany, March 24-27, 2009.

###### God Class
- Marios Fokaefs, Nikolaos Tsantalis, Eleni Stroulia, and Alexander Chatzigeorgiou, "[Identification and Application of Extract Class Refactorings in Object-Oriented Systems](http://users.encs.concordia.ca/~nikolaos/publications/JSS_2012.pdf)," Journal of Systems and Software, vol. 85, no. 10, pp. 2241–2260, October 2012.
- Marios Fokaefs, Nikolaos Tsantalis, Alexander Chatzigeorgiou, and Jörg Sander, "[Decomposing Object-Oriented Class Modules Using an Agglomerative Clustering Technique](http://users.encs.concordia.ca/~nikolaos/publications/ICSM_2009.pdf)," pp. 93-101, 25th IEEE International Conference on Software Maintenance (ICSM'2009), Edmonton, Alberta, Canada, September 20-26, 2009.
- Marios Fokaefs, Nikolaos Tsantalis, Eleni Stroulia, and Alexander Chatzigeorgiou, "[JDeodorant: Identification and Application of Extract Class Refactorings](http://users.encs.concordia.ca/~nikolaos/publications/ICSE_2011.pdf)," pp. 1037-1039, 33rd International Conference on Software Engineering (ICSE'2011), Tool Demonstration Session, Waikiki, Honolulu, Hawaii, USA, May 21-28, 2011.

###### Type/State Checking
- Nikolaos Tsantalis, and Alexander Chatzigeorgiou, "[Identification of Refactoring Opportunities Introducing Polymorphism](http://users.encs.concordia.ca/~nikolaos/publications/JSS_2010.pdf)," Journal of Systems and Software, vol. 83, no. 3, pp. 391-404, March 2010.
- Nikolaos Tsantalis, Theodoros Chaikalis, and Alexander Chatzigeorgiou, "[JDeodorant: Identification and Removal of Type-Checking Bad Smells](http://users.encs.concordia.ca/~nikolaos/publications/CSMR_2008.pdf)," pp. 329-331, 12th European Conference on Software Maintenance and Reengineering (CSMR'2008), Tool Demonstration Session, Athens, Greece, April 1-4, 2008.

###### Feature Envy
- Nikolaos Tsantalis, and Alexander Chatzigeorgiou, "[Identification of Move Method Refactoring Opportunities](http://users.encs.concordia.ca/~nikolaos/publications/TSE_2009.pdf)," IEEE Transactions on Software Engineering, vol. 35, no. 3, pp. 347-367, May/June 2009.
- Marios Fokaefs, Nikolaos Tsantalis, and Alexander Chatzigeorgiou, "[JDeodorant: Identification and Removal of Feature Envy Bad Smells](http://users.encs.concordia.ca/~nikolaos/publications/ICSM_2007.pdf)," pp. 519-520, 23rd IEEE International Conference on Software Maintenance (ICSM'2007), Tool Demonstration Session, Paris, France, October 2-5, 2007.

If you are using JDeodorant in your research, please **cite** at least one of the aforementioned papers.
