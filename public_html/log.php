<?php
	include_once 'dbConn.php';
	
	if(isset($_POST["url"])&&isset($_POST["query"])){
	$sql = "INSERT INTO click_log (ads_id, query) VALUES (' ".$_POST["docid"]."','".$_POST["query"]."')";
	$result=mysql_query($sql);
	
	$sql = "UPDATE ads_info SET num_view=num_view+1 WHERE id='".$_POST["docid"]."'";
	mysql_query($sql);
	mysql_close($conn);
	
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
