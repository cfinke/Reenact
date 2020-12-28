<!DOCTYPE html>
<html <?php language_attributes(); ?>>
	<head>
		<meta charset="<?php bloginfo( 'charset' ); ?>">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<title>Reenact photos with Reenact</title>
		<link rel="profile" href="http://gmpg.org/xfn/11">
		<?php wp_head(); ?>
		<link rel="shortcut icon" href="//reenact.me/wp-content/uploads/2015/11/reenact-256.png" />
		<script type="text/javascript">
			function nextImage() {
				var sliderImages = [
					"//reenact.me/wp-content/uploads/2015/11/intro1-574x1024.png",
					"//reenact.me/wp-content/uploads/2015/11/pick1-574x1024.png",
					"//reenact.me/wp-content/uploads/2015/11/capture-0-574x1024.png",
					"//reenact.me/wp-content/uploads/2015/11/capture-50-574x1024.png",
					"//reenact.me/wp-content/uploads/2015/11/capture-100-574x1024.png",
					"//reenact.me/wp-content/uploads/2015/11/confirm1-574x1024.png",
					"//reenact.me/wp-content/uploads/2015/11/share1-574x1024.png"
				];
				
				var appAnimation = document.getElementById( 'app-animation' );
				var currentIndex = parseInt( appAnimation.getAttribute( 'imageIndex' ), 10 );
				var nextIndex = ( currentIndex + 1 ) % sliderImages.length;
				appAnimation.setAttribute( 'imageIndex', nextIndex );
				appAnimation.src = sliderImages[ nextIndex ];

				setTimeout( nextImage, 4000 );

				var veryNextIndex = ( currentIndex + 2 ) % sliderImages.length;
				var veryNextImage = sliderImages[ nextIndex ];
				var preload = new Image();
				preload.src = veryNextImage;
			}
		</script>
		<style type="text/css">
			body {
				font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;
				font-size: 14px;
				line-height: 20px;
				color: #333;
				width: 100%;
			}

			h1 {
			    font-size: 48px;
			}

			h2 {
			    font-size: 36px;
			}

			a {
				color: #08C;
			}

			.install li {
				margin-top: 1em
			}

			#app-animation {
				max-width: 100%;
			}

			hr {
				border-width: 1px 0px;
				border-style: solid none;
				border-color: #EEE #333 #FFF;
				margin: 20px 0;
			}

			.jumbotron {
				text-align: center;
				width: 100%;
			}

			.jumbotron .content {
				text-align: left;
			}

			@media screen and (min-width: 500px) {
				h1 {
			    	font-size: 72px;
				}

				h2 {
			    	font-size: 48px;
				}
	
				.jumbotron {
					text-align: left;
				}

				#app-animation {
					float: left;
					max-width: 400px;
				}
	
				.jumbotron .content {
					padding-top: 45px;
					padding-right: 50px;
				}
	
				.jumbotron:after {
					content: " ";
					visibility: hidden;
					clear: both;
					display: table;
				}
	
				.container-narrow {
					max-width: 900px;
					margin-left: auto;
					margin-right: auto;
				}
			}

			@media screen and (max-width: 900px) {
				.container-narrow {
					max-width: 80%;
				}
			}

			@media screen and (max-width: 500px) {
				.container-narrow {
					max-width: 100%;
				}
	
				.jumbotron .content {
					padding: 0 10px;
				}
			}

			.get-it {
				font-size: 200%;
			}
		</style>
	</head>
	<body <?php body_class(); ?> onload="nextImage();">
		<div class="container-narrow">
			<div class="jumbotron">
				<img src="//reenact.me/wp-content/uploads/2015/11/intro1-574x1024.png" id="app-animation" imageIndex="-1" alt="Reenact is an app that makes it easy to reenact photos. Available for all capable Web browsers. Sorry, Safari users." />
				<div class="content">
					<h1>Reenact</h1>
					<p>Reenact is a web app for reenacting photos. Choose a photo to reenact, align the camera to match it, and take the shot.</p>
					<p>Reenact will even create a side-by-side comparison photo for easy sharing.</p>
					<p class="get-it"><a href="https://app.reenact.me/">Use it in your browser right now!</a></p>
					<p>It should work in all modern browsers, on desktop computers, tablets, and phones.</p>
					<p>Need help? Email <a href="mailto:help@reenact.me">help@reenact.me</a>.</p>
				</div>
			</div>
			<hr />
			<div>
				<h2>Reenacted Photos</h2>
				<?php echo do_shortcode( '[gallery ids="58,59,39,40,42,43,54,56"]' ); ?>
				<p>Send yours to <a href="mailto:gallery@reenact.me">gallery@reenact.me</a>!</p>
			</div>
			<hr/>
			<p style="text-align: center;"><small><a href="mailto:help@reenact.me">help@reenact.me</a></small></p>
		</div>
		<?php wp_footer(); ?>
	</body>
</html>
