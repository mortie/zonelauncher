function noop() {};

var gridSize = 25;

function random(min, max) {
	return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomVel() {
	switch (random(0, 3)) {
	case 0:
		return Vec(-1, 0);
	case 1:
		return Vec(1, 0);
	case 2:
		return Vec(0, -1);
	default:
		return Vec(0, 1);
	}
}

function randomPos(limit) {
	var w = Math.floor(window.innerWidth / gridSize);
	var h = Math.floor(window.innerHeight / gridSize);
	var qX = Math.floor(w / 4);
	var qY = Math.floor(h / 4);
	var minX, maxX, minY, maxY;

	if (limit) {
		minX = qX;
		maxX = w - qX;
		minY = qY;
		maxY = h - qY;
	} else {
		minX = 1;
		minY = 1;
		maxY = h - 1;
		maxX = w - 1;
	}
	return Vec(random(minX, maxX), random(minY, maxY));
}

function Vec($x, $y) {
	var self = {
		x: $x,
		y: $y
	};

	self.set = function(vec) {
		self.x = vec.x;
		self.y = vec.y;
	}

	self.add = function(vec) {
		self.x += vec.x;
		self.y += vec.y;
	}

	return self;
}

function Entity() {
	var self = {
		dead: false,
		pos: Vec(0, 0),
		update: noop,
		draw: noop,
	};

	self.die = function() { self.dead = true; }

	self.collidesWith = function(ent) {
		return (self.pos.x == ent.pos.x && self.pos.y == ent.pos.y);
	}

	return self;
}

function SnakePiece() {
	var self = Entity();

	var next = null;
	var interval = null;
	var vel = Vec(0, 0);

	self.setNext = function($next) {
		next = $next;
		next.setVel(vel);
		next.pos.set({
			x: self.pos.x - vel.x,
			y: self.pos.y - vel.y
		});
	}

	self.setVel = function($vel) {
		vel = $vel;
	}

	self.getVel = function() {
		return vel;
	}

	self.update = function($nextVel) {
		if (next) {
			next.update(vel);
		}
		if ($nextVel) {
			vel = $nextVel;
		}
		self.pos.add(vel);
	}

	self.draw = function($ctx) {
		$ctx.fillRect(
			self.pos.x * gridSize,
			self.pos.y * gridSize,
			gridSize,
			gridSize
		);
		$ctx.stroke();
		if (next) {
			next.draw($ctx);
		}
	}

	self.collidesWith = function(ent) {
		return (
			self.pos.x == ent.pos.x && self.pos.y == ent.pos.y ||
			(next && next.collidesWith(ent))
		);
	}

	self.collidesWithTail = function() {
		return next && next.collidesWith(self);
	}

	return self;
}

function Snake(keys, color) {
	var self = Entity();
	var firstPiece = SnakePiece();
	var lastPiece = firstPiece;
	firstPiece.pos.set(randomPos(true));
	firstPiece.setVel(randomVel());

	self.addPiece = function() {
		var piece = SnakePiece();
		lastPiece.setNext(piece);
		lastPiece = piece;
	}

	self.ateFruit = function() {
		self.addPiece();
		self.addPiece();
	}

	self.update = function($pressedKeys) {
		if ($pressedKeys[keys.up] && firstPiece.getVel().y != 1) {
			firstPiece.update({x: 0, y: -1});
		} else if ($pressedKeys[keys.down] && firstPiece.getVel().y != -1) {
			firstPiece.update({x: 0, y: 1});
		} else if ($pressedKeys[keys.left] && firstPiece.getVel().x != 1) {
			firstPiece.update({x: -1, y: 0});
		} else if ($pressedKeys[keys.right] && firstPiece.getVel().x != -1) {
			firstPiece.update({x: 1, y: 0});
		} else {
			firstPiece.update();
		}

		self.pos = firstPiece.pos;

		// Don't go out of the screen
		if ((self.pos.x < 0 || self.pos.x > (window.innerWidth / gridSize))
		||  (self.pos.y < 0 || self.pos.y > (window.innerHeight / gridSize))) {
			self.die();
		}

		// Don't collide with self
		if (firstPiece.collidesWithTail()) {
			self.die();
		}
	}

	self.draw = function($ctx) {
		$ctx.fillStyle = color;
		if (firstPiece) {
			firstPiece.draw($ctx);
		}
	}

	self.collidesWith = firstPiece.collidesWith;

	self.addPiece();
	self.addPiece();
	self.addPiece();
	self.addPiece();

	return self;
}


function Fruit() {
	var self = Entity();

	self.pos.set(randomPos());

	self.draw = function(ctx) {
		var x = (self.pos.x * gridSize) + (gridSize / 2);
		var y = (self.pos.y * gridSize) + (gridSize / 2);

		ctx.fillStyle = "rgb(220, 220, 0)";
		ctx.strokeStyle = "rgb(50, 50, 50)";
		ctx.beginPath();
		ctx.arc(x, y, gridSize * 0.25, 2 * Math.PI, false);
		ctx.closePath();
		ctx.fill();
		ctx.stroke();
	}

	return self;
}

function Board(canvas, ctx, snakes) {
	var self = {
		onexit: noop
	};

	var fruits = [Fruit()];

	var entityLists = [
		fruits,
		snakes
	];

	var timeout;
	var speed = 300;

	var pressedKeys = [];
	var toUnpress = [];
	function keydownListener($evt) {
		pressedKeys[$evt.keyCode] = true;
	}
	function keyupListener($evt) {
		toUnpress.push($evt.keyCode);
	}

	self.update = function() {
		timeout = setTimeout(self.update, speed);
		speed = Math.max(speed * 0.996, 100);

		canvas.width = window.innerWidth;
		canvas.height = window.innerHeight;

		// Update everything
		entityLists.forEach(function(list) {
			var toSplice = [];
			list.forEach(function(ent, i) {
				if (ent.dead) {
					toSplice.push(i);
					return;
				}

				ent.update(pressedKeys);

				ctx.save();
				ent.draw(ctx);
				ctx.restore();
			});

			// Delete dead things
			toSplice.forEach(function(index) {
				list.splice(index, 1);
			});
		});

		// Collide snakes
		snakes.forEach(function(snake) {
			snakes.forEach(function(snake2) {
				if (snake == snake2) return;
				if (snake2.collidesWith(snake))
					snake.die();
			});

		});
		// Stop if snakes are dead
		snakes.forEach(function(snake, i) {
			if (snake.dead) {
				self.stop(i);
			}
		});

		// Do fruit things
		fruits.forEach(function(fruit) {

			// Make snakes happy
			snakes.forEach(function(snake) {
				if (fruit.collidesWith(snake)) {
					fruit.die();
					snake.ateFruit();

					fruits.push(Fruit());
					if (random(0, 2) == 0) {
						fruits.push(Fruit());
					}
				}
			});
		});

		// Register keyups
		toUnpress.forEach(function(key) {
			delete pressedKeys[key];
		});
	}

	self.start = function() {
		window.addEventListener("keydown", keydownListener);
		window.addEventListener("keyup", keyupListener);
		self.update();
	}

	self.stop = function(player) {
		clearTimeout(timeout);
		window.removeEventListener("keydown", keydownListener);
		window.removeEventListener("keyup", keyupListener);
		self.onexit(player);
	}

	return self;
}

(function() {
	var canvas = document.querySelector("#canvas");
	var ctx = canvas.getContext("2d");

	var started = false;

	function start() {
		if (started) return;

		colors = [
			"rgb(255, 0, 0)",
			"rgb(0, 0, 255)"
		];

		var snakes = [
			Snake({up: 87, down: 83, left: 65, right: 68}, colors[0]),
			Snake({up: 38, down: 40, left: 37, right: 39}, colors[1])
		];

		var board = Board(canvas, ctx, snakes);
		board.start();
		started = true;

		board.onexit = function(player) {
			console.log("exited");
			started = false;
			canvas.width = canvas.width;
			ctx.save();
			ctx.textAlign = "center";
			ctx.font = "48px sans-serif";
			ctx.fillStyle = colors[player];
			ctx.fillText(
				"Player "+(player + 1)+" lost!",
				canvas.width / 2,
				canvas.height / 2
			);
			ctx.fillStyle = "rgb(0, 0, 0)";
			ctx.fillText(
				"Press Enter to restart.",
				canvas.width / 2,
				canvas.height / 2 + 100
			);
			ctx.restore();
		}
	}

	start();

	window.addEventListener("keydown", function(evt) {
		if (evt.keyCode == 13) {
			start();
		}
	});
})();
