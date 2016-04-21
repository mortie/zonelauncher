from Input import Input
import pygame
from pygame import draw
import time
import sys
import os

class Sounds:
    ball_bounce = None

    def load(self, name):
        return pygame.mixer.Sound(os.path.join("assets", name+".ogg"));

    def __init__(self):
        self.ball_bounce = self.load("ball_bounce")

class Sides:
    LEFT = 0
    RIGHT = 1
    TOP = 2
    BOT = 3

class Entity:
    x = 0
    y = 0
    vx = 0
    vy = 0
    width = 0
    height = 0
    speed = 1

    class Rect:
        left = 0
        right = 0
        top = 0
        bot = 0

    def update(self):
        if self.vx != 0:
            self.x += self.vx * self.speed
        if self.vy != 0:
            self.y += self.vy * self.speed

    def draw(self, screen):
        pass

    # Get rekt
    def getrect(self):
        rect = self.Rect()
        rect.left = self.x - self.width / 2
        rect.right = self.x + self.width / 2
        rect.top = self.y + self.height / 2
        rect.bot = self.y - self.height / 2
        return rect

    def intersects(self, entity):
        r1 = self.getrect()
        r2 = entity.getrect()
        return (
            r1.left < r2.right and r1.right > r2.left and
            r1.top > r2.bot and r1.bot < r2.top
         )

class Pad(Entity):
    side = None
    input = None
    _width = 20
    _height = 100
    distFromEdge = 20
    game = None
    speed = 1
    _x = 0
    _y = 0
    edgeX = False
    edgeY = False
    color = (255, 255, 255)

    @property
    def width(self):
        if self._width == "100%":
            return self.game.width
        else:
            return self._width

    @property
    def height(self):
        if self._height == "100%":
            return self.game.height
        else:
            return self._height

    @property
    def x(self):
        if self._x == "50%":
            return self.game.width / 2
        elif self.edgeX and self._x < 0:
            return self._x + self.game.width
        else:
            return self._x
    @x.setter
    def x(self, val):
        self._x = val

    @property
    def y(self):
        if self._y == "50%":
            return self.game.height / 2
        elif self.edgeY and self._y < 0:
            return self._y + self.game.height
        else:
            return self._y
    @y.setter
    def y(self, val):
        self._y = val

    def __init__(self, side, input, game):
        self.side = side
        self.input = input
        self.game = game

        # Make the pad take the entire screen if there's no input
        if input == None:
            self._height = "100%"

        # Swap width and height if necessary
        if side in (Sides.TOP, Sides.BOT):
            self._width, self._height = self._height, self._width

        # Set X and Y values
        if side in (Sides.LEFT, Sides.RIGHT):
            self.edgeX = True
            self._y = "50%"
            if side == Sides.LEFT:
                self._x = self.distFromEdge
            else:
                self._x = -self.distFromEdge
        else:
            self.edgeY = True
            self._x = "50%"
            if side == Sides.TOP:
                self._y = self.distFromEdge
            else:
                self._y = -self.distFromEdge

    def update(self):
        if self.input != None:
            if self.side in (Sides.LEFT, Sides.RIGHT):
                self.vy = -self.input.val
            else:
                self.vx = -self.input.val

        if self.x <= 0: self.x = 0
        elif self.x > self.game.width: self.x = self.game.width
        if self.y <= 0: self.y = 1
        elif self.y > self.game.height: self.y = self.game.height

        super().update()

    def draw(self, screen):
        halfw = self.width / 2
        halfh = self.height / 2

        x = self.x
        y = self.y

        # 1       4
        #     x
        #   2   3

        slope = 15
        points = None
        if self.side == Sides.LEFT:
            points = (
                (x - halfw, y + halfh + slope),
                (x + halfw, y + halfh),
                (x + halfw, y - halfh),
                (x - halfw, y - halfh - slope)
            )
        elif self.side == Sides.RIGHT:
            points = (
                (x + halfw, y + halfh + slope),
                (x - halfw, y + halfh),
                (x - halfw, y - halfh),
                (x + halfw, y - halfh - slope)
            )
        elif self.side == Sides.TOP:
            points = (
                (x - halfw - slope, y - halfh),
                (x - halfw, y + halfh),
                (x + halfw, y + halfh),
                (x + halfw + slope, y - halfh)
            )
        elif self.side == Sides.BOT:
            points = (
                (x - halfw - slope, y + halfh),
                (x - halfw, y - halfh),
                (x + halfw, y - halfh),
                (x + halfw + slope, y + halfh)
            )

        draw.polygon(
            screen,
            self.color,
            points
        )

class Ball(Entity):
    vx = 1
    vy = 1
    rad = 10
    game = None
    speed = 0.7

    def __init__(self, game):
        self.width = self.rad * 2
        self.height = self.rad * 2
        self.x = game.width / 2
        self.y = game.height / 2
        self.game = game

    def draw(self, screen):
        draw.circle(
            screen,
            (255, 255, 255),
            (int(self.x), int(self.y)),
            self.rad
        )

    def update(self):
        # Bounce on pads
        bounced = False
        bouncedTip = False
        for entity in self.game.entities:
            if isinstance(entity, Pad) and self.intersects(entity):
                bounced = True

                # Left and right
                if entity.side == Sides.LEFT:
                    self.vx = abs(self.vx)
                elif entity.side == Sides.RIGHT:
                    self.vx = -abs(self.vx)

                # Top and bottom
                if entity.side == Sides.TOP:
                    self.vy = abs(self.vy)
                elif entity.side == Sides.BOT:
                    self.vy = -abs(self.vy)

                # Left and right tip bounce
                if entity.side in (Sides.LEFT, Sides.RIGHT):
                    if self.y > entity.y + entity.height / 2:
                        bouncedTip = True
                        self.vy = abs(self.vy) + (entity.vy * 0.3)
                    elif self.y < entity.y - entity.height / 2:
                        bouncedTip = True
                        self.vy = -abs(self.vy) + (entity.vy * 0.3)

                # Top and bot tip bounce
                if entity.side in (Sides.TOP, Sides.BOT):
                    if self.x > entity.x + entity.width / 2:
                        bouncedTip = True
                        self.vx = abs(self.vx) + (entity.vx * 0.3)
                    elif self.x < entity.x - entity.width / 2:
                        bouncedTip = True
                        self.vx = -abs(self.vx) + (entity.vx * 0.3)

        if bouncedTip:
            self.x += (self.vx * 6)
            self.y += (self.vy * 6)

        if bounced:
            self.game.sounds.ball_bounce.play()
            self.speed += 0.02

        # Lose
        if self.x < 0:
            self.game.lose(Sides.LEFT)
        elif self.x > self.game.width:
            self.game.lose(Sides.RIGHT)
        elif self.y < 0:
            self.game.lose(Sides.BOT)
        elif self.y > self.game.height:
            self.game.lose(Sides.TOP)

        super().update()

class Game:
    entities = []

    screen = None
    sounds = None

    width = 0
    height = 0

    running = True
    loser = None

    def __init__(self, screen, inputs):
        self.entities = []
        self.screen = screen
        self.width, self.height = screen.get_size()

        # Audio
        self.sounds = Sounds()

        for i in range(0, 4 - len(inputs)):
            inputs.append(None)

        self.entities.append(Pad(Sides.LEFT, inputs[0], self))
        self.entities.append(Pad(Sides.RIGHT, inputs[1], self))
        self.entities.append(Pad(Sides.TOP, inputs[2], self))
        self.entities.append(Pad(Sides.BOT, inputs[3], self))

        self.entities.append(Ball(self))

    def run(self):
        self.gameloop()
        return self.loser

    def gameloop(self):
        while self.running:

            events = pygame.event.get()

            # Dispatch events to inputs
            for entity in self.entities:
                if isinstance(entity, Pad) and entity.input != None:
                    entity.input.update()
                    for event in events:
                        entity.input.onevent(event)

            # Handle close method and such
            for event in events:
                if event.type == pygame.QUIT:
                    pygame.quit()
                    sys.exit()

            self.update()
            time.sleep((1 / 60) / 1000)

    def update(self):
        for entity in self.entities:
            entity.update()

        self.screen.fill((0, 0, 0))
        for entity in self.entities:
            entity.draw(self.screen)

        pygame.display.update()

    def lose(self, side):
        self.loser = -1

        for entity in self.entities:
            if isinstance(entity, Pad) and entity.side == side and entity.input != None:
                self.loser = entity.input.playerId

        self.running = False
