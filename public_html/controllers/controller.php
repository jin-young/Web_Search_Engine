<?php
	include_once("../models/dbModel.php");
	
	$request = $_GET["request"];
	$company = $_GET["company"];
	$title = $_GET["title"];
	$url = $_GET["url"];
	$content = $_GET["content"];
	$cost = $_GET["cost"];
	switch($request){
		case "register":
			$result = register($company, $title, $url, $content, $cost);
			echo $result;
			break;
	}

?>