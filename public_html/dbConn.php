<?php
	$user = "jj1233";
	$password = "sw9kc24m";
	$host = "warehouse.cims.nyu.edu";
	$database = "jj1233_search_engine";
	$conn = mysql_pconnect($host, $user, $password) or die(mysql_error());
	mysql_select_db($database, $conn) or die(mysql_error());
?>