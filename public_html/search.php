<html>
<head>
<meta charset="utf-8">

<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script type="text/javascript"
	src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.11.0/jquery.validate.js"></script>
<script type="text/javascript" src="./scripts/g04.js"></script>
<link rel="stylesheet" href="./styles/g04.css" type="text/css" media="screen" />

<?php 
	$query = str_replace(' ', '%20',$_GET["query"]);
	$query = str_replace('"', '%22', $query);
	echo '<script type="text/javascript">
			getSearchResult('.$query.');
			getAdsResult('.$query.');
		  </script>';
?>

</head>	
<body>
	<div>
		<a href="main.html"><img align="absmiddle" width="100" height="30"
			src="./image/google.png"></a> <input type="text" name="query"
			style="width: 600; height:30" /> <img align="absmiddle" width="100" height="30"
			src="./image/searchbutton.png">
	</div>
	<hr />
	<?php echo $query; ?>
	<div id="result_body"></div>
</body>
</html>