<?php

		$user = "root";
		$password = "root";
		$host = "localhost";
		$database = "websearch";
		$conn = mysql_pconnect($host, $user, $password) or die(mysql_error());
		mysql_select_db($database, $conn) or die(mysql_error());

	if(isset($_POST["url"])&&isset($_POST["query"])){
	$sql = "INSERT INTO click_log (ads_id, query) VALUES (' ".$_POST["url"]."','".$_POST["query"]."')";
	$result=mysql_query($sql);
	header('Location:'.$_POST["url"]);
	}
	else{
		header('Location:search.php');
	}
?>
<html>
<head>
</head>	
<body>
</body>
</html>
