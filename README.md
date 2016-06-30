Mimacom Vert.x 3 Tech Session
================

A real-time web minigame demonstrating the use of server-side Java 8 and Vert.x ( http://vertx.io/ ) and client-side Javascript. Used as an example at Mimacom tech session.

The server side consists of more Verticles. Main verticle is ConfigVerticle which initializes other verticles with default config values

Client side is made with Javascript http://www.html5quintus.com/#demo HTML5 engine.

NOTICE: This is by no means a complete fool-proof implementation of the game. Some topics that would require more attention in production-quality software:
 - Data efficiency: every frame is calculated on the server side and sent as a real-time update (every ~20ms). This may cause unnecessary lags on slower/high ping connections.
   More efficient way would be only to send change-state messages (ball has hit a wall, player has hit/missed the ball etc...) and let client-side javascript do the simple movement interpolation
 - Security: Current security implementation relies on clients not knowing other players' or games' GUID. GUID is supposed to be unique, so we assume there are no collisions (this is ok), but
   if someone finds out GUID of some other player, he could influence his games and act on his behalf. In a real world, authentication/authorization/"session" mechanisms would be needed
 - and some more I can't currently think of... :o)

For TechSession attendees, please fill out the feedback form here: https://goo.gl/7m55Ii 